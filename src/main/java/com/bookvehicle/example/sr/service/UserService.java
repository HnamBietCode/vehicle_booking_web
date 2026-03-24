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
    private final DriverLicenseRepository driverLicenseRepository;
    private final UserSessionRepository userSessionRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final RatingRepository ratingRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final VehicleRentalRepository vehicleRentalRepository;
    private final VehicleRepository vehicleRepository;
    private final SoberBookingRepository soberBookingRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository,
            CustomerRepository customerRepository,
            DriverRepository driverRepository,
            DriverLicenseRepository driverLicenseRepository,
            UserSessionRepository userSessionRepository,
            DeviceTokenRepository deviceTokenRepository,
            NotificationRepository notificationRepository,
            RatingRepository ratingRepository,
            WithdrawalRequestRepository withdrawalRequestRepository,
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            VehicleRentalRepository vehicleRentalRepository,
            VehicleRepository vehicleRepository,
            SoberBookingRepository soberBookingRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.driverRepository = driverRepository;
        this.driverLicenseRepository = driverLicenseRepository;
        this.userSessionRepository = userSessionRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationRepository = notificationRepository;
        this.ratingRepository = ratingRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.vehicleRentalRepository = vehicleRentalRepository;
        this.vehicleRepository = vehicleRepository;
        this.soberBookingRepository = soberBookingRepository;
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

    @Transactional(readOnly = true)
    public List<DriverLicense> findDriverLicenses(Long driverId) {
        return driverLicenseRepository.findByDriverId(driverId);
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
                if (form.getVehicleTypes() != null && !form.getVehicleTypes().isBlank()) {
                    d.setVehicleTypes(form.getVehicleTypes().trim());
                }
                driverRepository.save(d);

                // Update or create license class in driver_licenses table
                if (form.getLicenseClass() != null && !form.getLicenseClass().isBlank()) {
                    java.util.List<DriverLicense> licenses = driverLicenseRepository.findByDriverId(d.getId());
                    if (!licenses.isEmpty()) {
                        DriverLicense dl = licenses.get(0);
                        dl.setLicenseClass(form.getLicenseClass().trim());
                        if (form.getVehicleTypes() != null && !form.getVehicleTypes().isBlank()) {
                            dl.setVehicleTypes(form.getVehicleTypes().trim());
                        }
                        driverLicenseRepository.save(dl);
                    } else {
                        DriverLicense dl = new DriverLicense();
                        dl.setDriverId(d.getId());
                        dl.setLicenseNumber(d.getDriverLicense());
                        dl.setLicenseClass(form.getLicenseClass().trim());
                        dl.setLicenseExpiry(d.getLicenseExpiry());
                        dl.setVehicleTypes(form.getVehicleTypes() != null ? form.getVehicleTypes().trim() : d.getVehicleTypes());
                        driverLicenseRepository.save(dl);
                    }
                }
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
    public String adminCreateUser(RegisterForm form,
            String[] licenseNumbers, String[] licenseClasses, String[] licenseExpiries, String[] licenseVehicleTypes) {

        // ── Validate Email ──
        if (form.getEmail() == null || form.getEmail().isBlank())
            return "Email không được để trống.";
        if (form.getEmail().length() > 100)
            return "Email không được vượt quá 100 ký tự.";
        if (!form.getEmail().matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"))
            return "Email không đúng định dạng (ví dụ: example@email.com).";
        if (userRepository.existsByEmail(form.getEmail()))
            return "Email đã được sử dụng.";

        // ── Validate Số điện thoại ──
        if (form.getPhone() == null || form.getPhone().isBlank())
            return "Số điện thoại không được để trống.";
        if (!form.getPhone().matches("^0[0-9]{9}$"))
            return "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0 (ví dụ: 0901234567).";
        if (userRepository.existsByPhone(form.getPhone()))
            return "Số điện thoại đã được sử dụng.";

        // ── Validate Họ và tên ──
        if (form.getFullName() == null || form.getFullName().isBlank())
            return "Họ và tên không được để trống.";
        if (form.getFullName().trim().length() < 2 || form.getFullName().trim().length() > 100)
            return "Họ và tên phải từ 2 đến 100 ký tự.";

        // ── Validate Mật khẩu ──
        if (form.getPassword() == null || form.getPassword().isBlank())
            return "Mật khẩu không được để trống.";
        if (form.getPassword().length() < 6)
            return "Mật khẩu phải từ 6 ký tự trở lên.";
        if (!form.getPassword().matches(".*[A-Z].*"))
            return "Mật khẩu phải chứa ít nhất 1 chữ cái viết hoa.";
        if (!form.getPassword().matches(".*[0-9].*"))
            return "Mật khẩu phải chứa ít nhất 1 chữ số.";
        if (!form.getPassword().equals(form.getConfirmPassword()))
            return "Xác nhận mật khẩu không khớp.";

        // ── Validate Vai trò ──
        Role role;
        try {
            role = Role.valueOf(form.getRole().toUpperCase());
        } catch (Exception e) {
            return "Loại tài khoản không hợp lệ.";
        }

        // ── Validate CCCD (DRIVER) ──
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

        User user = new User();
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPhone(form.getPhone().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(role);
        user.setIsActive(role != Role.DRIVER);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());
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
            d.setCccd(form.getCccd().trim());
            d.setDriverLicense("PENDING");
            d.setLicenseExpiry(java.time.LocalDate.now().plusYears(1));
            d.setVehicleTypes("CAR_4");
            d.setVerificationStatus(VerificationStatus.PENDING);
            driverRepository.save(d);

            // ── Validate + Lưu danh sách bằng lái ──
            if (licenseNumbers != null && licenseNumbers.length > 0) {
                // Validate từng bằng lái trước khi lưu
                for (int i = 0; i < licenseNumbers.length; i++) {
                    if (licenseNumbers[i] == null || licenseNumbers[i].isBlank()) continue;
                    if (licenseNumbers[i].trim().length() > 12)
                        return "Số bằng lái không được vượt quá 12 ký tự (bằng lái #" + (i + 1) + ").";
                    if (licenseExpiries != null && i < licenseExpiries.length && !licenseExpiries[i].isBlank()) {
                        java.time.LocalDate expiry = java.time.LocalDate.parse(licenseExpiries[i]);
                        if (expiry.isBefore(java.time.LocalDate.now()))
                            return "Hạn bằng lái không được là ngày trong quá khứ (bằng lái #" + (i + 1) + ").";
                    }
                }

                java.util.Set<String> allVehicleTypes = new java.util.LinkedHashSet<>();
                java.time.LocalDate latestExpiry = null;
                String firstLicenseNumber = null;

                for (int i = 0; i < licenseNumbers.length; i++) {
                    if (licenseNumbers[i] == null || licenseNumbers[i].isBlank()) continue;

                    DriverLicense dl = new DriverLicense();
                    dl.setDriverId(d.getId());
                    dl.setLicenseNumber(licenseNumbers[i].trim());
                    dl.setLicenseClass(licenseClasses != null && i < licenseClasses.length ? licenseClasses[i] : "B2");
                    dl.setLicenseExpiry(licenseExpiries != null && i < licenseExpiries.length && !licenseExpiries[i].isBlank()
                            ? java.time.LocalDate.parse(licenseExpiries[i]) : java.time.LocalDate.now().plusYears(1));
                    dl.setVehicleTypes(licenseVehicleTypes != null && i < licenseVehicleTypes.length && licenseVehicleTypes[i] != null
                            ? licenseVehicleTypes[i] : "CAR_4");
                    driverLicenseRepository.save(dl);

                    for (String vt : dl.getVehicleTypes().split(",")) {
                        if (!vt.isBlank()) allVehicleTypes.add(vt.trim());
                    }
                    if (firstLicenseNumber == null) firstLicenseNumber = dl.getLicenseNumber();
                    if (latestExpiry == null || dl.getLicenseExpiry().isAfter(latestExpiry)) {
                        latestExpiry = dl.getLicenseExpiry();
                    }
                }

                if (!allVehicleTypes.isEmpty()) d.setVehicleTypes(String.join(",", allVehicleTypes));
                if (firstLicenseNumber != null) d.setDriverLicense(firstLicenseNumber);
                if (latestExpiry != null) d.setLicenseExpiry(latestExpiry);
                driverRepository.save(d);
            }
        }
        return null;
    }

    /**
     * Admin sửa thông tin user.
     */
    public String adminUpdateUser(Long userId, ProfileEditForm form, String role, Boolean isActive, String newPassword,
            String[] licenseNumbers, String[] licenseClasses, String[] licenseExpiries, String[] licenseVehicleTypes) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty())
            return "Không tìm thấy tài khoản.";
        User user = optUser.get();

        // ── Validate Số điện thoại ──
        if (form.getPhone() == null || form.getPhone().isBlank())
            return "Số điện thoại không được để trống.";
        if (!form.getPhone().matches("^0[0-9]{9}$"))
            return "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0 (ví dụ: 0901234567).";
        if (!user.getPhone().equals(form.getPhone()) && userRepository.existsByPhone(form.getPhone()))
            return "Số điện thoại đã được sử dụng.";

        // ── Validate Họ và tên ──
        if (form.getFullName() == null || form.getFullName().isBlank())
            return "Họ và tên không được để trống.";
        if (form.getFullName().trim().length() < 2 || form.getFullName().trim().length() > 100)
            return "Họ và tên phải từ 2 đến 100 ký tự.";

        // ── Validate + Đổi mật khẩu nếu admin nhập ──
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 6)
                return "Mật khẩu mới phải từ 6 ký tự trở lên.";
            if (!newPassword.matches(".*[A-Z].*"))
                return "Mật khẩu mới phải chứa ít nhất 1 chữ cái viết hoa.";
            if (!newPassword.matches(".*[0-9].*"))
                return "Mật khẩu mới phải chứa ít nhất 1 chữ số.";
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        user.setPhone(form.getPhone().trim());
        if (form.getAvatarUrl() != null && !form.getAvatarUrl().isBlank())
            user.setAvatarUrl(form.getAvatarUrl().trim());
        if (isActive != null)
            user.setIsActive(isActive);
        if (role != null && !role.isBlank()) {
            try {
                Role newRole = Role.valueOf(role.toUpperCase());
                if (newRole == Role.DRIVER) {
                    if (form.getCccd() == null || form.getCccd().isBlank()) {
                        return "Căn cước công dân (CCCD) không được để trống khi chọn vai trò tài xế.";
                    }
                    if (!form.getCccd().matches("^[0-9]{12}$")) {
                        return "Căn cước công dân (CCCD) phải bao gồm đúng 12 chữ số.";
                    }
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
                newD.setDriverLicense("PENDING");
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

            // ── Validate + Xử lý danh sách bằng lái ──
            if (licenseNumbers != null && licenseNumbers.length > 0) {
                // Validate từng bằng lái trước khi lưu
                for (int i = 0; i < licenseNumbers.length; i++) {
                    if (licenseNumbers[i] == null || licenseNumbers[i].isBlank()) continue;
                    if (licenseNumbers[i].trim().length() > 12)
                        return "Số bằng lái không được vượt quá 12 ký tự (bằng lái #" + (i + 1) + ").";
                    if (licenseExpiries != null && i < licenseExpiries.length && !licenseExpiries[i].isBlank()) {
                        java.time.LocalDate expiry = java.time.LocalDate.parse(licenseExpiries[i]);
                        if (expiry.isBefore(java.time.LocalDate.now()))
                            return "Hạn bằng lái không được là ngày trong quá khứ (bằng lái #" + (i + 1) + ").";
                    }
                }

                driverLicenseRepository.deleteByDriverId(d.getId());
                java.util.Set<String> allVehicleTypes = new java.util.LinkedHashSet<>();
                java.time.LocalDate latestExpiry = null;
                String firstLicenseNumber = null;

                for (int i = 0; i < licenseNumbers.length; i++) {
                    if (licenseNumbers[i] == null || licenseNumbers[i].isBlank()) continue;

                    DriverLicense dl = new DriverLicense();
                    dl.setDriverId(d.getId());
                    dl.setLicenseNumber(licenseNumbers[i].trim());
                    dl.setLicenseClass(licenseClasses != null && i < licenseClasses.length ? licenseClasses[i] : "B2");
                    dl.setLicenseExpiry(licenseExpiries != null && i < licenseExpiries.length && !licenseExpiries[i].isBlank()
                            ? java.time.LocalDate.parse(licenseExpiries[i]) : java.time.LocalDate.now().plusYears(1));
                    dl.setVehicleTypes(licenseVehicleTypes != null && i < licenseVehicleTypes.length && licenseVehicleTypes[i] != null
                            ? licenseVehicleTypes[i] : "CAR_4");
                    driverLicenseRepository.save(dl);

                    // Gom vehicle types
                    for (String vt : dl.getVehicleTypes().split(",")) {
                        if (!vt.isBlank()) allVehicleTypes.add(vt.trim());
                    }
                    if (firstLicenseNumber == null) firstLicenseNumber = dl.getLicenseNumber();
                    if (latestExpiry == null || dl.getLicenseExpiry().isAfter(latestExpiry)) {
                        latestExpiry = dl.getLicenseExpiry();
                    }
                }

                // Cập nhật tổng hợp vào driver
                if (!allVehicleTypes.isEmpty()) {
                    d.setVehicleTypes(String.join(",", allVehicleTypes));
                }
                if (firstLicenseNumber != null) d.setDriverLicense(firstLicenseNumber);
                if (latestExpiry != null) d.setLicenseExpiry(latestExpiry);
                driverRepository.save(d);
            }
        }
        return null;
    }

    /**
     * Admin xoá user.
     */
    public void delete(Long userId) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) return;
        User user = optUser.get();

        // 1. Xóa sessions & device tokens
        userSessionRepository.deleteByUserId(userId);
        deviceTokenRepository.deleteByUserId(userId);

        // 2. Xóa notifications
        notificationRepository.deleteByUserId(userId);

        // 3. Xóa ratings (reviewer)
        ratingRepository.deleteByReviewerId(userId);

        // 4. Xóa withdrawal requests
        withdrawalRequestRepository.deleteByUserId(userId);

        // 5. Xóa transactions & wallet
        transactionRepository.deleteByWalletUserId(userId);
        walletRepository.deleteByUserId(userId);

        // 6. Xóa các bản ghi phụ thuộc driver/customer
        if (user.getRole() == Role.DRIVER) {
            driverRepository.findByUserId(userId).ifPresent(driver -> {
                // Gỡ driver khỏi vehicle_rentals (set null để không bị FK error)
                vehicleRentalRepository.clearDriverId(driver.getId());
                // Gỡ assigned_driver khỏi vehicles
                vehicleRepository.clearAssignedDriver(driver.getId());
                // Xóa driver licenses
                driverLicenseRepository.deleteByDriverId(driver.getId());
                // Xóa sober bookings của driver
                soberBookingRepository.clearDriverId(driver.getId());
                // Xóa driver
                driverRepository.delete(driver);
            });
        }

        if (user.getRole() == Role.CUSTOMER) {
            customerRepository.findByUserId(userId).ifPresent(customer -> {
                // Xóa vehicle_rentals của customer
                vehicleRentalRepository.deleteByCustomerId(customer.getId());
                // Xóa sober bookings của customer
                soberBookingRepository.deleteByCustomerId(customer.getId());
                // Xóa customer
                customerRepository.delete(customer);
            });
        }

        // 7. Cuối cùng xóa user
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
                driver.setApprovedAt(null);
                driver.setApprovedBy(null);
            }
            driverRepository.save(driver);

            // Khi duyệt → kích hoạt tài khoản, khi từ chối → KHÔNG khóa tài khoản
            Optional<User> optUser = userRepository.findById(userId);
            if (optUser.isPresent()) {
                User user = optUser.get();
                if (vs == VerificationStatus.APPROVED) {
                    user.setIsActive(true);
                    userRepository.save(user);
                }
                // REJECTED: không lock account — tài xế vẫn đăng nhập được nhưng không thể nhận đơn
            }

            return null;
        } catch (Exception e) {
            return "Trạng thái phê duyệt không hợp lệ.";
        }
    }
}
