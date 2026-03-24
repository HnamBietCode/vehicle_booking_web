package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.RegisterForm;
import com.bookvehicle.example.sr.model.Customer;
import com.bookvehicle.example.sr.model.Driver;
import com.bookvehicle.example.sr.model.MembershipType;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.VerificationStatus;
import com.bookvehicle.example.sr.repository.CustomerRepository;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final Map<String, String> passwordResetOtpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> passwordResetOtpExpiry = new ConcurrentHashMap<>();
    private final Map<String, String> registrationOtpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> registrationOtpExpiry = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       DriverRepository driverRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.emailService = emailService;
    }

    public RegistrationResult register(RegisterForm form) {
        String validationError = validateRegisterForm(form);
        if (validationError != null) {
            return RegistrationResult.error(validationError);
        }

        Role role = Role.valueOf(form.getRole().toUpperCase());

        User user = new User();
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPhone(form.getPhone().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(role);
        user.setIsActive(false);
        user.setEmailVerified(false);
        user.setEmailVerifiedAt(null);
        User savedUser = userRepository.save(user);

        if (role == Role.CUSTOMER) {
            Customer customer = new Customer();
            customer.setUserId(savedUser.getId());
            customer.setFullName(form.getFullName().trim());
            customer.setMembership(MembershipType.STANDARD);
            customerRepository.save(customer);
        } else {
            Driver driver = new Driver();
            driver.setUserId(savedUser.getId());
            driver.setFullName(form.getFullName().trim());
            driver.setCccd(form.getCccd().trim());
            driver.setDriverLicense(form.getDriverLicense().trim());
            driver.setLicenseExpiry(form.getLicenseExpiry());
            driver.setVehicleTypes(form.getVehicleTypes().trim());
            driver.setVerificationStatus(VerificationStatus.PENDING);
            driverRepository.save(driver);
        }

        sendRegistrationOtpInternal(savedUser.getEmail(), role == Role.DRIVER);
        return RegistrationResult.success(savedUser.getEmail(), role);
    }

    @Transactional(readOnly = true)
    public LoginResult attemptLogin(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            return LoginResult.error("Email hoặc mật khẩu không đúng.");
        }

        Optional<User> opt = userRepository.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) {
            return LoginResult.error("Email hoặc mật khẩu không đúng.");
        }

        User user = opt.get();
        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            return LoginResult.error("Email hoặc mật khẩu không đúng.");
        }
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            return LoginResult.error("Email chưa được xác thực. Vui lòng nhập mã OTP đã gửi về Gmail của bạn.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            if (user.getRole() == Role.DRIVER) {
                return LoginResult.error("Email đã xác thực nhưng hồ sơ tài xế vẫn đang chờ Admin duyệt.");
            }
            return LoginResult.error("Tài khoản của bạn hiện không thể đăng nhập.");
        }

        return LoginResult.success(user);
    }

    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) {
        LoginResult result = attemptLogin(email, rawPassword);
        return result.ok() ? result.user() : null;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public String resendRegistrationOtp(String email) {
        Optional<User> opt = findByEmail(email);
        if (opt.isEmpty()) {
            return "Không tìm thấy tài khoản cần xác thực.";
        }

        User user = opt.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return "Email này đã được xác thực trước đó.";
        }

        sendRegistrationOtpInternal(user.getEmail(), user.getRole() == Role.DRIVER);
        return null;
    }

    public String verifyRegistrationOtp(String email, String otp) {
        Optional<User> opt = findByEmail(email);
        if (opt.isEmpty()) {
            return "Không tìm thấy tài khoản cần xác thực.";
        }

        User user = opt.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return null;
        }

        String key = email.trim().toLowerCase();
        String storedOtp = registrationOtpStorage.get(key);
        Long expiryTime = registrationOtpExpiry.get(key);

        if (storedOtp == null || expiryTime == null) {
            return "Mã OTP không hợp lệ hoặc đã hết hạn.";
        }
        if (System.currentTimeMillis() > expiryTime) {
            registrationOtpStorage.remove(key);
            registrationOtpExpiry.remove(key);
            return "Mã OTP đã hết hạn.";
        }
        if (!storedOtp.equals(otp)) {
            return "Mã OTP không chính xác.";
        }

        registrationOtpStorage.remove(key);
        registrationOtpExpiry.remove(key);

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        if (user.getRole() == Role.CUSTOMER) {
            user.setIsActive(true);
        }
        userRepository.save(user);
        return null;
    }

    public String sendPasswordResetOtp(String email) {
        Optional<User> opt = findByEmail(email);
        if (opt.isEmpty()) {
            return "Email không tồn tại trong hệ thống.";
        }

        String normalizedEmail = opt.get().getEmail();
        String otp = generateOtp();
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);

        passwordResetOtpStorage.put(normalizedEmail, otp);
        passwordResetOtpExpiry.put(normalizedEmail, expiryTime);
        emailService.sendPasswordResetOtp(normalizedEmail, otp);
        return null;
    }

    public String verifyPasswordResetOtp(String email, String otp) {
        String key = email.trim().toLowerCase();
        String storedOtp = passwordResetOtpStorage.get(key);
        Long expiryTime = passwordResetOtpExpiry.get(key);

        if (storedOtp == null || expiryTime == null) {
            return "Mã OTP không hợp lệ hoặc đã hết hạn.";
        }
        if (System.currentTimeMillis() > expiryTime) {
            passwordResetOtpStorage.remove(key);
            passwordResetOtpExpiry.remove(key);
            return "Mã OTP đã hết hạn.";
        }
        if (!storedOtp.equals(otp)) {
            return "Mã OTP không chính xác.";
        }

        return null;
    }

    public String resetPassword(String email, String otp, String newPassword, String confirmPassword) {
        String verifyErr = verifyPasswordResetOtp(email, otp);
        if (verifyErr != null) {
            return verifyErr;
        }

        if (newPassword == null || newPassword.length() < 6) {
            return "Mật khẩu mới phải từ 6 ký tự trở lên.";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "Xác nhận mật khẩu không khớp.";
        }

        Optional<User> optUser = findByEmail(email);
        if (optUser.isEmpty()) {
            return "Người dùng không tồn tại.";
        }

        User user = optUser.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetOtpStorage.remove(user.getEmail());
        passwordResetOtpExpiry.remove(user.getEmail());
        return null;
    }

    private String validateRegisterForm(RegisterForm form) {
        if (form.getFullName() == null || form.getFullName().isBlank()) {
            return "Họ tên không được để trống.";
        }
        if (form.getFullName().trim().length() < 2) {
            return "Họ tên phải có ít nhất 2 ký tự.";
        }
        if (form.getFullName().trim().length() > 100) {
            return "Họ tên không được vượt quá 100 ký tự.";
        }

        if (form.getEmail() == null || form.getEmail().isBlank()) {
            return "Email không được để trống.";
        }
        if (form.getEmail().trim().length() > 100) {
            return "Email không được vượt quá 100 ký tự.";
        }
        if (!form.getEmail().trim().matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return "Email không đúng định dạng.";
        }

        if (form.getPhone() == null || form.getPhone().isBlank()) {
            return "Số điện thoại không được để trống.";
        }
        String phone = form.getPhone().trim().replaceAll("\\s+", "");
        if (!phone.matches("^0[0-9]{9,10}$")) {
            return "Số điện thoại không hợp lệ (phải bắt đầu bằng 0, gồm 10-11 chữ số).";
        }
        form.setPhone(phone);

        if (form.getPassword() == null || form.getPassword().length() < 6) {
            return "Mật khẩu phải từ 6 ký tự trở lên.";
        }
        if (form.getPassword().length() > 50) {
            return "Mật khẩu không được vượt quá 50 ký tự.";
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            return "Xác nhận mật khẩu không khớp.";
        }

        String normalizedEmail = form.getEmail().trim().toLowerCase();
        form.setEmail(normalizedEmail);
        if (userRepository.existsByEmail(normalizedEmail)) {
            return "Email đã được sử dụng.";
        }
        if (userRepository.existsByPhone(phone)) {
            return "Số điện thoại đã được sử dụng.";
        }

        Role role;
        try {
            role = Role.valueOf(form.getRole().toUpperCase());
            if (role == Role.ADMIN) {
                return "Không thể tự đăng ký tài khoản Admin.";
            }
        } catch (Exception e) {
            return "Loại tài khoản không hợp lệ.";
        }

        if (role == Role.DRIVER) {
            if (form.getCccd() == null || form.getCccd().isBlank()) {
                return "Căn cước công dân (CCCD) không được để trống khi đăng ký tài xế.";
            }
            if (!form.getCccd().matches("^[0-9]{12}$")) {
                return "Căn cước công dân (CCCD) phải bao gồm đúng 12 chữ số.";
            }
            if (driverRepository.existsByCccd(form.getCccd())) {
                return "Căn cước công dân (CCCD) đã tồn tại.";
            }

            if (form.getDriverLicense() == null || form.getDriverLicense().isBlank()) {
                return "Bằng lái xe không được để trống.";
            }
            if (form.getDriverLicense().trim().length() > 12) {
                return "Số bằng lái xe không được vượt quá 12 ký tự.";
            }
            if (form.getLicenseExpiry() == null) {
                return "Ngày hết hạn bằng lái không được để trống.";
            }
            if (form.getLicenseExpiry().isBefore(LocalDate.now())) {
                return "Bằng lái xe đã hết hạn.";
            }
            if (form.getVehicleTypes() == null || form.getVehicleTypes().isBlank()) {
                return "Loại xe không được để trống.";
            }
        }

        return null;
    }

    private void sendRegistrationOtpInternal(String email, boolean driverAccount) {
        String normalizedEmail = email.trim().toLowerCase();
        String otp = generateOtp();
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000);
        registrationOtpStorage.put(normalizedEmail, otp);
        registrationOtpExpiry.put(normalizedEmail, expiryTime);
        emailService.sendRegistrationOtp(normalizedEmail, otp, driverAccount);
    }

    private String generateOtp() {
        return String.format("%06d", new java.util.Random().nextInt(999999));
    }

    public record RegistrationResult(boolean ok, String message, String email, Role role) {
        public static RegistrationResult success(String email, Role role) {
            return new RegistrationResult(true, null, email, role);
        }

        public static RegistrationResult error(String message) {
            return new RegistrationResult(false, message, null, null);
        }
    }

    public record LoginResult(boolean ok, String message, User user) {
        public static LoginResult success(User user) {
            return new LoginResult(true, null, user);
        }

        public static LoginResult error(String message) {
            return new LoginResult(false, message, null);
        }
    }
}
