package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.ChangePasswordForm;
import com.bookvehicle.example.sr.dto.ProfileEditForm;
import com.bookvehicle.example.sr.dto.RegisterForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       DriverRepository driverRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.driverRepository = driverRepository;
    }

    // ── Read ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findCustomerByUserId(Long userId) {
        return customerRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Driver> findDriverByUserId(Long userId) {
        return driverRepository.findByUserId(userId);
    }

    // ── Update Profile ──────────────────────────────────────────────
    /**
     * Cập nhật hồ sơ cá nhân.
     * @return null nếu thành công, chuỗi lỗi nếu thất bại.
     */
    public String updateProfile(Long userId, ProfileEditForm form) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return "Không tìm thấy tài khoản.";
        User user = optUser.get();

        // Kiểm tra phone trùng
        if (!user.getPhone().equals(form.getPhone()) &&
                userRepository.existsByPhone(form.getPhone())) {
            return "Số điện thoại đã được sử dụng bởi tài khoản khác.";
        }

        user.setPhone(form.getPhone().trim());
        if (form.getAvatarUrl() != null && !form.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(form.getAvatarUrl().trim());
        }
        userRepository.save(user);

        // Cập nhật fullName trong Customer / Driver
        if (user.getRole() == Role.CUSTOMER) {
            customerRepository.findByUserId(userId).ifPresent(c -> {
                c.setFullName(form.getFullName().trim());
                if (form.getAddress() != null) c.setAddress(form.getAddress().trim());
                customerRepository.save(c);
            });
        } else if (user.getRole() == Role.DRIVER) {
            driverRepository.findByUserId(userId).ifPresent(d -> {
                d.setFullName(form.getFullName().trim());
                driverRepository.save(d);
            });
        }
        return null;
    }

    // ── Change Password ─────────────────────────────────────────────
    /**
     * @return null nếu thành công, chuỗi lỗi nếu thất bại.
     */
    public String changePassword(Long userId, ChangePasswordForm form) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return "Không tìm thấy tài khoản.";
        User user = optUser.get();

        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword()))
            return "Mật khẩu hiện tại không đúng.";
        if (form.getNewPassword() == null || form.getNewPassword().length() < 6)
            return "Mật khẩu mới phải từ 6 ký tự trở lên.";
        if (!form.getNewPassword().equals(form.getConfirmPassword()))
            return "Xác nhận mật khẩu mới không khớp.";

        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);
        return null;
    }

    // ── Admin CRUD ──────────────────────────────────────────────────

    /**
     * Admin tạo user mới (có thể tạo ADMIN).
     */
    public String adminCreateUser(RegisterForm form) {
        if (userRepository.existsByEmail(form.getEmail()))
            return "Email đã được sử dụng.";
        if (userRepository.existsByPhone(form.getPhone()))
            return "Số điện thoại đã được sử dụng.";

        Role role;
        try { role = Role.valueOf(form.getRole().toUpperCase()); }
        catch (Exception e) { return "Loại tài khoản không hợp lệ."; }

        User user = new User();
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPhone(form.getPhone().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(role);
        user.setIsActive(true);
        User saved = userRepository.save(user);

        if (role == Role.CUSTOMER) {
            Customer c = new Customer();
            c.setUserId(saved.getId());
            c.setFullName(form.getFullName().trim());
            c.setMembership(MembershipType.STANDARD);
            customerRepository.save(c);
        } else if (role == Role.DRIVER) {
            Driver d = new Driver();
            d.setUserId(saved.getId());
            d.setFullName(form.getFullName().trim());
            d.setCccd("000000000000");
            d.setDriverLicense("000000000000");
            d.setLicenseExpiry(java.time.LocalDate.now().plusYears(1));
            d.setVehicleTypes("CAR_4");
            d.setVerificationStatus(VerificationStatus.PENDING);
            driverRepository.save(d);
        }
        return null;
    }

    /**
     * Admin sửa thông tin user.
     */
    public String adminUpdateUser(Long userId, ProfileEditForm form, String role, Boolean isActive) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return "Không tìm thấy tài khoản.";
        User user = optUser.get();

        if (!user.getPhone().equals(form.getPhone()) && userRepository.existsByPhone(form.getPhone()))
            return "Số điện thoại đã được sử dụng.";

        user.setPhone(form.getPhone().trim());
        if (form.getAvatarUrl() != null && !form.getAvatarUrl().isBlank())
            user.setAvatarUrl(form.getAvatarUrl().trim());
        if (isActive != null) user.setIsActive(isActive);
        if (role != null && !role.isBlank()) {
            try { user.setRole(Role.valueOf(role.toUpperCase())); }
            catch (Exception ignored) {}
        }
        userRepository.save(user);

        if (user.getRole() == Role.CUSTOMER) {
            customerRepository.findByUserId(userId).ifPresent(c -> {
                c.setFullName(form.getFullName().trim());
                if (form.getAddress() != null) c.setAddress(form.getAddress().trim());
                customerRepository.save(c);
            });
        } else if (user.getRole() == Role.DRIVER) {
            driverRepository.findByUserId(userId).ifPresent(d -> {
                d.setFullName(form.getFullName().trim());
                driverRepository.save(d);
            });
        }
        return null;
    }

    /**
     * Admin xoá user.
     */
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }

    /**
     * Khoá / mở khoá tài khoản.
     */
    public void toggleActive(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setIsActive(!u.getIsActive());
            userRepository.save(u);
        });
    }
}
