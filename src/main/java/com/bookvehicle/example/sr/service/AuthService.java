package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.RegisterForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
            CustomerRepository customerRepository,
            DriverRepository driverRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Đăng ký tài khoản mới (CUSTOMER hoặc DRIVER).
     * 
     * @return thông báo lỗi nếu thất bại, null nếu thành công.
     */
    public String register(RegisterForm form) {
        // Validate
        if (form.getFullName() == null || form.getFullName().isBlank())
            return "Họ tên không được để trống.";
        if (form.getEmail() == null || form.getEmail().isBlank())
            return "Email không được để trống.";
        if (form.getPhone() == null || form.getPhone().isBlank())
            return "Số điện thoại không được để trống.";
        if (form.getPassword() == null || form.getPassword().length() < 6)
            return "Mật khẩu phải từ 6 ký tự trở lên.";
        if (!form.getPassword().equals(form.getConfirmPassword()))
            return "Xác nhận mật khẩu không khớp.";
        if (userRepository.existsByEmail(form.getEmail()))
            return "Email đã được sử dụng.";
        if (userRepository.existsByPhone(form.getPhone()))
            return "Số điện thoại đã được sử dụng.";

        Role role;
        try {
            role = Role.valueOf(form.getRole().toUpperCase());
            if (role == Role.ADMIN)
                return "Không thể tự đăng ký tài khoản Admin.";
        } catch (Exception e) {
            return "Loại tài khoản không hợp lệ.";
        }

        // Nếu là DRIVER, yêu cầu phải nhập đủ thông tin chuyên ngành
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
            if (form.getLicenseExpiry() == null) {
                return "Ngày hết hạn bằng lái không được để trống.";
            }
            if (form.getLicenseExpiry().isBefore(java.time.LocalDate.now())) {
                return "Bằng lái xe đã hết hạn.";
            }
            if (form.getVehicleTypes() == null || form.getVehicleTypes().isBlank()) {
                return "Loại xe không được để trống.";
            }
        }

        // Tạo User
        User user = new User();
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPhone(form.getPhone().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(role);
        // Tài xế đăng ký thì isActive = false để đợi admin duyệt
        user.setIsActive(role != Role.DRIVER);
        User savedUser = userRepository.save(user);

        // Tạo profile phụ
        if (role == Role.CUSTOMER) {
            Customer c = new Customer();
            c.setUserId(savedUser.getId());
            c.setFullName(form.getFullName().trim());
            c.setMembership(MembershipType.STANDARD);
            customerRepository.save(c);
        } else { // DRIVER
            Driver d = new Driver();
            d.setUserId(savedUser.getId());
            d.setFullName(form.getFullName().trim());

            // Lấy dữ liệu chuyên ngành từ form
            d.setCccd(form.getCccd().trim());
            d.setDriverLicense(form.getDriverLicense().trim());
            d.setLicenseExpiry(form.getLicenseExpiry());
            d.setVehicleTypes(form.getVehicleTypes().trim());

            d.setVerificationStatus(VerificationStatus.PENDING);
            driverRepository.save(d);
        }

        return null; // Thành công
    }

    /**
     * Xác thực đăng nhập.
     * 
     * @return User nếu hợp lệ, null nếu sai thông tin.
     */
    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) {
        Optional<User> opt = userRepository.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty())
            return null;
        User user = opt.get();
        if (!user.getIsActive())
            return null;
        if (!passwordEncoder.matches(rawPassword, user.getPassword()))
            return null;
        return user;
    }

    // ── Forgot Password Logic ───────────────────────────────────────
    
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();
    
    public String sendPasswordResetOtp(String email) {
        if (!userRepository.existsByEmail(email.trim().toLowerCase())) {
            return "Email không tồn tại trong hệ thống.";
        }
        
        // Generate random 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        
        otpStorage.put(email.toLowerCase(), otp);
        otpExpiry.put(email.toLowerCase(), expiryTime);
        
        System.out.println("==================================================");
        System.out.println("OTP YÊU CẦU ĐẶT LẠI MẬT KHẨU CHO " + email + " LÀ: " + otp);
        System.out.println("==================================================");
        
        return null; // Success
    }

    public String verifyPasswordResetOtp(String email, String otp) {
        String storedOtp = otpStorage.get(email.toLowerCase());
        Long expiryTime = otpExpiry.get(email.toLowerCase());
        
        if (storedOtp == null || expiryTime == null) {
            return "Mã OTP không hợp lệ hoặc đã hết hạn.";
        }
        if (System.currentTimeMillis() > expiryTime) {
            otpStorage.remove(email.toLowerCase());
            otpExpiry.remove(email.toLowerCase());
            return "Mã OTP đã hết hạn.";
        }
        if (!storedOtp.equals(otp)) {
            return "Mã OTP không chính xác.";
        }
        
        return null;
    }

    public String resetPassword(String email, String otp, String newPassword, String confirmPassword) {
        String verifyErr = verifyPasswordResetOtp(email, otp);
        if (verifyErr != null) return verifyErr;
        
        if (newPassword == null || newPassword.length() < 6)
            return "Mật khẩu mới phải từ 6 ký tự trở lên.";
        if (!newPassword.equals(confirmPassword))
            return "Xác nhận mật khẩu không khớp.";
            
        Optional<User> optUser = userRepository.findByEmail(email.toLowerCase());
        if (optUser.isEmpty()) return "Người dùng không tồn tại.";
        
        User user = optUser.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        otpStorage.remove(email.toLowerCase());
        otpExpiry.remove(email.toLowerCase());
        
        return null;
    }
}
