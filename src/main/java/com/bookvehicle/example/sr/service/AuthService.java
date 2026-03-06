package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.RegisterForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
            if (role == Role.ADMIN) return "Không thể tự đăng ký tài khoản Admin.";
        } catch (Exception e) {
            return "Loại tài khoản không hợp lệ.";
        }

        // Nếu là DRIVER, yêu cầu phải nhập CCCD hợp lệ (đúng 12 số)
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
        }

        // Tạo User
        User user = new User();
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPhone(form.getPhone().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(role);
        user.setIsActive(true);
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
            
            // Lấy CCCD từ form (đã qua vòng check ở trên)
            d.setCccd(form.getCccd().trim());
            
            // Vẫn dùng ID để tạo placeholder cho Giấy Phép Lái Xe vì màn đăng ký ban đầu chưa yêu cầu
            String uniquePlaceholder = String.format("%012d", savedUser.getId());
            d.setDriverLicense(uniquePlaceholder); 

            d.setLicenseExpiry(java.time.LocalDate.now().plusYears(1));
            d.setVehicleTypes("CAR_4");
            d.setVerificationStatus(VerificationStatus.PENDING);
            driverRepository.save(d);
        }

        return null; // Thành công
    }

    /**
     * Xác thực đăng nhập.
     * @return User nếu hợp lệ, null nếu sai thông tin.
     */
    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) {
        Optional<User> opt = userRepository.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) return null;
        User user = opt.get();
        if (!user.getIsActive()) return null;
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) return null;
        return user;
    }
}
