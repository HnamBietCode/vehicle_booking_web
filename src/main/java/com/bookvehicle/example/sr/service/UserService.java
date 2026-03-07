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
     * 
     * @return null nếu thành công, chuỗi lỗi nếu thất bại.
     */
    public String updateProfile(Long userId, ProfileEditForm form) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty())
            return "Không tìm thấy tài khoản.";
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
                if (form.getAddress() != null)
                    c.setAddress(form.getAddress().trim());
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
        if (optUser.isEmpty())
            return "Không tìm thấy tài khoản.";
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
        try {
            role = Role.valueOf(form.getRole().toUpperCase());
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

        User user = new User();
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPhone(form.getPhone().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(role);
        // Tài xế thì mặc định isActive = false để chờ duyệt
        user.setIsActive(role != Role.DRIVER);
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

            // Lấy CCCD từ form (đã qua vòng check ở trên)
            d.setCccd(form.getCccd().trim());

            // Vẫn dùng ID để tạo placeholder cho Giấy Phép Lái Xe vì màn đăng ký ban đầu
            // chưa yêu cầu
            String uniquePlaceholder = String.format("%012d", saved.getId());
            d.setDriverLicense(uniquePlaceholder);

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
        if (optUser.isEmpty())
            return "Không tìm thấy tài khoản.";
        User user = optUser.get();

        if (!user.getPhone().equals(form.getPhone()) && userRepository.existsByPhone(form.getPhone()))
            return "Số điện thoại đã được sử dụng.";

        user.setPhone(form.getPhone().trim());
        if (form.getAvatarUrl() != null && !form.getAvatarUrl().isBlank())
            user.setAvatarUrl(form.getAvatarUrl().trim());
        if (isActive != null)
            user.setIsActive(isActive);
        if (role != null && !role.isBlank()) {
            try {
                Role newRole = Role.valueOf(role.toUpperCase());
                // Validate cccd if changing to driver
                if (newRole == Role.DRIVER) {
                    if (form.getCccd() == null || form.getCccd().isBlank()) {
                        return "Căn cước công dân (CCCD) không được để trống khi chọn vai trò tài xế.";
                    }
                    if (!form.getCccd().matches("^[0-9]{12}$")) {
                        return "Căn cước công dân (CCCD) phải bao gồm đúng 12 chữ số.";
                    }
                    // Prevent duplicate CCCD unless it belongs to the same driver
                    Optional<Driver> existingDriver = driverRepository.findByUserId(userId);
                    if (existingDriver.isEmpty() || !existingDriver.get().getCccd().equals(form.getCccd())) {
                        if (driverRepository.existsByCccd(form.getCccd())) {
                            return "Căn cước công dân (CCCD) đã tồn tại.";
                        }
                    }
                }
                user.setRole(newRole);
            } catch (Exception ignored) {
            }
        }
        userRepository.save(user);

        if (user.getRole() == Role.CUSTOMER) {
            Customer c = customerRepository.findByUserId(userId).orElseGet(() -> {
                Customer newC = new Customer();
                newC.setUserId(userId);
                newC.setMembership(MembershipType.STANDARD);
                return newC;
            });
            c.setFullName(form.getFullName().trim());
            if (form.getAddress() != null)
                c.setAddress(form.getAddress().trim());
            customerRepository.save(c);
        } else if (user.getRole() == Role.DRIVER) {
            Driver d = driverRepository.findByUserId(userId).orElseGet(() -> {
                Driver newD = new Driver();
                newD.setUserId(userId);
                newD.setDriverLicense(String.format("%012d", userId));
                newD.setLicenseExpiry(java.time.LocalDate.now().plusYears(1));
                newD.setVehicleTypes("CAR_4");
                newD.setVerificationStatus(VerificationStatus.PENDING);
                return newD;
            });
            d.setFullName(form.getFullName().trim());
            if (form.getCccd() != null && !form.getCccd().isBlank()) {
                d.setCccd(form.getCccd().trim());
            }
            driverRepository.save(d);
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
     * Khoá / mở khoá tài khoản có tuỳ chọn lý do.
     */
    public void toggleActive(Long userId, String reason) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setIsActive(!u.getIsActive());
            userRepository.save(u);

            if (!u.getIsActive() && u.getRole() == Role.DRIVER) {
                driverRepository.findByUserId(userId).ifPresent(d -> {
                    d.setRejectionReason(reason);
                    driverRepository.save(d);
                });
            }
        });
    }

    /**
     * Admin duyệt/từ chối hồ sơ tài xế.
     */
    public String verifyDriver(Long userId, String status, Long adminId, String reason) {
        Optional<Driver> optDriver = driverRepository.findByUserId(userId);
        if (optDriver.isEmpty())
            return "Không tìm thấy hồ sơ tài xế.";

        try {
            VerificationStatus vs = VerificationStatus.valueOf(status.toUpperCase());
            Driver driver = optDriver.get();
            driver.setVerificationStatus(vs);

            if (vs == VerificationStatus.APPROVED) {
                driver.setApprovedAt(java.time.LocalDateTime.now());
                driver.setApprovedBy(adminId);
                driver.setRejectionReason(null);
            } else if (vs == VerificationStatus.REJECTED) {
                driver.setRejectionReason(reason);
            }
            driverRepository.save(driver);

            // Cập nhật trạng thái User
            Optional<User> optUser = userRepository.findById(userId);
            if (optUser.isPresent()) {
                User user = optUser.get();
                if (vs == VerificationStatus.APPROVED) {
                    user.setIsActive(true);
                    userRepository.save(user);
                } else if (vs == VerificationStatus.REJECTED) {
                    user.setIsActive(false);
                    userRepository.save(user);
                }
            }

            return null;
        } catch (Exception e) {
            return "Trạng thái phê duyệt không hợp lệ.";
        }
    }
}
