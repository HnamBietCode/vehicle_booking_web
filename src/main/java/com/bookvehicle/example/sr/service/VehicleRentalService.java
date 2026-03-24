package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.RentalCreateForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VehicleRentalService {

    private final VehicleRentalRepository vehicleRentalRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final PickupPointRepository pickupPointRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PushNotificationService pushNotificationService;
    private final BookingRealtimeService bookingRealtimeService;
    private final GeocodingService geocodingService;
    private final NotificationService notificationService;

    public VehicleRentalService(
            VehicleRentalRepository vehicleRentalRepository,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            PickupPointRepository pickupPointRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            WalletService walletService,
            PushNotificationService pushNotificationService,
            BookingRealtimeService bookingRealtimeService,
            GeocodingService geocodingService,
            NotificationService notificationService) {
        this.vehicleRentalRepository = vehicleRentalRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.pickupPointRepository = pickupPointRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.pushNotificationService = pushNotificationService;
        this.bookingRealtimeService = bookingRealtimeService;
        this.geocodingService = geocodingService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<VehicleRental> findByCustomerUserId(Long userId) {
        return customerRepository.findByUserId(userId)
                .map(customer -> vehicleRentalRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<VehicleRental> findByDriverUserId(Long userId) {
        return driverRepository.findByUserId(userId)
                .map(driver -> vehicleRentalRepository.findByDriverIdOrderByCreatedAtDesc(driver.getId()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Optional<VehicleRental> findById(Long rentalId) {
        return vehicleRentalRepository.findById(rentalId);
    }

    @Transactional(readOnly = true)
    public List<VehicleRental> findPendingVehicleOnly() {
        return vehicleRentalRepository.findPendingVehicleOnly();
    }

    @Transactional(readOnly = true)
    public List<VehicleRental> findAllPending() {
        return vehicleRentalRepository.findAllPending();
    }

    public ServiceResult createRental(Long customerUserId, RentalCreateForm form) {
        Optional<Customer> customerOpt = customerRepository.findByUserId(customerUserId);
        if (customerOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay thong tin khach hang.");
        }

        // Ràng buộc: chỉ được thuê 1 xe 1 lần, phải hoàn thành + thanh toán mới được thuê tiếp
        Customer customer = customerOpt.get();
        List<VehicleRental> activeRentals = vehicleRentalRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        for (VehicleRental existing : activeRentals) {
            VehicleRental.RentalStatus status = existing.getStatus();
            // Đang có đơn chưa hoàn thành (PENDING, CONFIRMED, ACTIVE)
            if (status == VehicleRental.RentalStatus.PENDING 
                || status == VehicleRental.RentalStatus.CONFIRMED 
                || status == VehicleRental.RentalStatus.ACTIVE) {
                return ServiceResult.error("Bạn đang có đơn thuê xe chưa hoàn thành (đơn #" + existing.getId() + "). Vui lòng hoàn thành hoặc hủy đơn cũ trước khi thuê xe mới.");
            }
            // Đã hoàn thành nhưng chưa thanh toán
            if (status == VehicleRental.RentalStatus.COMPLETED 
                && existing.getPaymentStatus() != PaymentStatus.PAID) {
                return ServiceResult.error("Bạn có đơn thuê xe #" + existing.getId() + " đã hoàn thành nhưng chưa thanh toán. Vui lòng thanh toán trước khi thuê xe mới.");
            }
        }

        if (form.getVehicleId() == null) {
            return ServiceResult.error("Vui long chon xe.");
        }
        if (form.getPlannedStart() == null || form.getPlannedEnd() == null) {
            return ServiceResult.error("Vui long chon thoi gian bat dau va ket thuc.");
        }
        if (!form.getPlannedEnd().isAfter(form.getPlannedStart())) {
            return ServiceResult.error("Thoi gian ket thuc phai sau bat dau.");
        }

        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(form.getVehicleId());
        if (vehicleOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay xe.");
        }
        Vehicle vehicle = vehicleOpt.get();
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            return ServiceResult.error("Xe hien tai khong san sang.");
        }

        List<VehicleRental.RentalStatus> busyStatuses = List.of(
                VehicleRental.RentalStatus.PENDING,
                VehicleRental.RentalStatus.CONFIRMED,
                VehicleRental.RentalStatus.ACTIVE
        );
        if (vehicleRentalRepository.hasVehicleOverlap(
                vehicle.getId(), form.getPlannedStart(), form.getPlannedEnd(), busyStatuses)) {
            return ServiceResult.error("Xe da co lich trong khoang thoi gian nay.");
        }

        VehicleRental.RentalMode mode = form.getRentalMode() == null
                ? VehicleRental.RentalMode.WITH_DRIVER
                : form.getRentalMode();

        Optional<Driver> assignedDriver = Optional.empty();
        if (mode == VehicleRental.RentalMode.WITH_DRIVER) {
            assignedDriver = getAssignedDriverForVehicle(vehicle, form.getPlannedStart(), form.getPlannedEnd(), busyStatuses);
            if (assignedDriver.isEmpty()) {
                return ServiceResult.error("Xe chua co tai xe ranh phu hop.");
            }
        }
        BigDecimal basePrice = calculateBasePrice(vehicle, form.getRentalType(), form.getPlannedStart(), form.getPlannedEnd());

        Long pickupPointId = null;
        String pickupAddress;
        if (form.getPickupPointId() != null) {
            Optional<PickupPoint> pointOpt = pickupPointRepository.findByIdAndIsActiveTrue(form.getPickupPointId());
            if (pointOpt.isEmpty()) {
                return ServiceResult.error("Diem don da chon khong hop le.");
            }
            pickupPointId = pointOpt.get().getId();
            pickupAddress = pointOpt.get().getAddress();
        } else {
            if (form.getPickupAddress() == null || form.getPickupAddress().isBlank()) {
                return ServiceResult.error("Vui long chon diem don co san hoac tu nhap dia chi.");
            }
            pickupAddress = form.getPickupAddress().trim();
        }

        // Geocode pickup address to lat/lng for map display
        Double pickupLat = null;
        Double pickupLng = null;
        if (pickupPointId != null) {
            // Use PickupPoint coordinates if available
            PickupPoint point = pickupPointRepository.findById(pickupPointId).orElse(null);
            if (point != null && point.getLatitude() != null && point.getLongitude() != null) {
                pickupLat = point.getLatitude().doubleValue();
                pickupLng = point.getLongitude().doubleValue();
            }
        }
        if (pickupLat == null) {
            if (form.getPickupLat() != null && form.getPickupLng() != null) {
                pickupLat = form.getPickupLat();
                pickupLng = form.getPickupLng();
            } else {
                // Geocode manual address
                try {
                    GeocodingService.LatLng coords = geocodingService.geocode(pickupAddress, null, null, null);
                    if (coords != null) {
                        pickupLat = coords.lat();
                        pickupLng = coords.lng();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        VehicleRental rental = new VehicleRental();
        rental.setCustomerId(customerOpt.get().getId());
        rental.setVehicleId(vehicle.getId());
        rental.setDriverId(assignedDriver.map(Driver::getId).orElse(null));
        rental.setPickupPointId(pickupPointId);
        rental.setPickupAddress(pickupAddress);
        rental.setPickupLat(pickupLat);
        rental.setPickupLng(pickupLng);
        rental.setRentalType(form.getRentalType());
        rental.setRentalMode(mode);
        rental.setPlannedStart(form.getPlannedStart());
        rental.setPlannedEnd(form.getPlannedEnd());
        rental.setBasePrice(basePrice);
        rental.setExtraFee(BigDecimal.ZERO);
        rental.setDiscountAmount(BigDecimal.ZERO);
        rental.setTotalPrice(basePrice);
        rental.setStatus(VehicleRental.RentalStatus.PENDING);
        rental.setPaymentStatus(PaymentStatus.PENDING);
        rental.setNotes(form.getNotes());

        vehicleRentalRepository.save(rental);
        if (mode == VehicleRental.RentalMode.VEHICLE_ONLY) {
            notifyAvailableDriversForVehicleOnly(rental, vehicle);
        }
        // In-app notification cho customer
        try {
            notificationService.createNotification(
                    customerUserId, "Đặt xe thành công",
                    "Bạn đã đặt xe " + vehicle.getName() + ". Chờ tài xế xác nhận.",
                    NotificationType.RENTAL_CREATED,
                    NotificationRefType.RENTAL, rental.getId());
            // Thông báo cho tài xế nếu có
            if (assignedDriver.isPresent()) {
                notificationService.createNotification(
                        assignedDriver.get().getUserId(), "Đơn thuê xe mới",
                        "Bạn có đơn thuê xe mới. Kiểm tra và xác nhận ngay.",
                        NotificationType.BOOKING_ASSIGNED,
                        NotificationRefType.RENTAL, rental.getId());
            }
        } catch (Exception ignored) {}
        return ServiceResult.success("Dat xe thanh cong. Cho tai xe xac nhan.", rental.getId());
    }

    private void notifyAvailableDriversForVehicleOnly(VehicleRental rental, Vehicle vehicle) {
        try {
            List<Driver> drivers = driverRepository.findAvailableByVehicleType(vehicle.getCategory().name());
            List<Long> userIds = drivers.stream().map(Driver::getUserId).toList();
            pushNotificationService.notifyUsers(
                    userIds,
                    "Co don giao xe moi",
                    "Khach hang can giao xe. Bam de nhan don.",
                    java.util.Map.of("tripType", "RENTAL", "tripId", String.valueOf(rental.getId()))
            );
        } catch (Exception ignored) {
        }
    }

    public ServiceResult acceptRental(Long rentalId, Long driverUserId) {
        Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay don thue.");
        }
        VehicleRental rental = rentalOpt.get();
        if (rental.getStatus() != VehicleRental.RentalStatus.PENDING) {
            return ServiceResult.error("Don thue khong o trang thai cho xac nhan.");
        }

        Optional<Driver> actorDriver = driverRepository.findByUserId(driverUserId);
        if (actorDriver.isEmpty()) {
            return ServiceResult.error("Ban khong co quyen nhan don nay.");
        }

        Driver driver = actorDriver.get();
        if (!Boolean.TRUE.equals(driver.getIsAvailable())) {
            return ServiceResult.error("Tai xe dang ban, khong the nhan don.");
        }

        if (rental.getRentalMode() == VehicleRental.RentalMode.WITH_DRIVER) {
            if (rental.getDriverId() == null || !driver.getId().equals(rental.getDriverId())) {
                return ServiceResult.error("Ban khong co quyen nhan don nay.");
            }
        } else {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(rental.getVehicleId());
            if (vehicleOpt.isEmpty()) {
                return ServiceResult.error("Khong tim thay xe.");
            }
            if (!isDriverEligible(driver, vehicleOpt.get().getCategory())) {
                return ServiceResult.error("Tai xe khong phu hop loai xe.");
            }
            if (vehicleRentalRepository.hasDriverOverlap(driver.getId(),
                    rental.getPlannedStart(), rental.getPlannedEnd(),
                    List.of(VehicleRental.RentalStatus.PENDING, VehicleRental.RentalStatus.CONFIRMED, VehicleRental.RentalStatus.ACTIVE))) {
                return ServiceResult.error("Tai xe da co lich trong khoang thoi gian nay.");
            }
            rental.setDriverId(driver.getId());
        }

        rental.setStatus(VehicleRental.RentalStatus.CONFIRMED);
        vehicleRentalRepository.save(rental);

        driver.setIsAvailable(false);
        driverRepository.save(driver);
        bookingRealtimeService.publishRentalUpdate(
                rental,
                "DRIVER_ACCEPTED",
                "Tai xe da nhan don cua ban. Ban co the bat dau theo doi ngay khi tai xe chia se vi tri."
        );
        // In-app notification for customer
        try {
            Customer cust = customerRepository.findById(rental.getCustomerId()).orElse(null);
            if (cust != null) {
                notificationService.createNotification(
                        cust.getUserId(), "Tài xế xác nhận đơn thuê",
                        "Đơn thuê xe #" + rental.getId() + " đã được tài xế xác nhận.",
                        NotificationType.RENTAL_ACCEPTED,
                        NotificationRefType.RENTAL, rental.getId());
            }
        } catch (Exception ignored) {}
        return ServiceResult.success("Da nhan don thue.", rental.getId());
    }

    public ServiceResult rejectRental(Long rentalId, Long driverUserId, String reason) {
        Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay don thue.");
        }
        VehicleRental rental = rentalOpt.get();

        // Cho phép hủy khi đơn ở trạng thái PENDING, CONFIRMED hoặc ACTIVE
        if (rental.getStatus() != VehicleRental.RentalStatus.PENDING
            && rental.getStatus() != VehicleRental.RentalStatus.CONFIRMED
            && rental.getStatus() != VehicleRental.RentalStatus.ACTIVE) {
            return ServiceResult.error("Đơn thuê đã hoàn thành hoặc đã bị hủy, không thể hủy.");
        }

        Optional<Driver> actorDriver = driverRepository.findByUserId(driverUserId);
        if (actorDriver.isEmpty() || rental.getDriverId() == null || !actorDriver.get().getId().equals(rental.getDriverId())) {
            return ServiceResult.error("Bạn không có quyền hủy đơn này.");
        }

        // ── Hoàn tiền nếu khách đã thanh toán ─────────────────────────
        boolean wasActive = rental.getStatus() == VehicleRental.RentalStatus.ACTIVE
                         || rental.getStatus() == VehicleRental.RentalStatus.CONFIRMED;
        if (rental.getPaymentStatus() == PaymentStatus.PAID && rental.getTotalPrice() != null
                && rental.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
            // Hoàn tiền cho khách hàng
            Customer customer = customerRepository.findById(rental.getCustomerId()).orElse(null);
            if (customer != null) {
                walletService.credit(
                        customer.getUserId(),
                        rental.getTotalPrice(),
                        TransactionType.REFUND,
                        ReferenceType.RENTAL,
                        rental.getId(),
                        "Hoàn tiền đơn thuê xe #" + rental.getId() + " do tài xế hủy"
                );
                // Thu lại tiền từ tài xế (nếu đã chia)
                BigDecimal driverShare = rental.getTotalPrice()
                        .multiply(new BigDecimal("0.60"))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                try {
                    walletService.debit(
                            driverUserId,
                            driverShare,
                            TransactionType.REFUND,
                            ReferenceType.RENTAL,
                            rental.getId(),
                            "Thu lại tiền hoa hồng đơn #" + rental.getId() + " do hủy"
                    );
                } catch (Exception ignored) { /* driver may not have enough balance */ }
            }
            rental.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        rental.setStatus(VehicleRental.RentalStatus.CANCELLED);
        rental.setCancelReason((reason == null || reason.isBlank()) ? "Tài xế hủy đơn." : reason.trim());
        vehicleRentalRepository.save(rental);

        // Giải phóng tài xế
        driverRepository.findById(rental.getDriverId()).ifPresent(d -> {
            d.setIsAvailable(true);
            driverRepository.save(d);
        });
        // Giải phóng xe
        vehicleRepository.findById(rental.getVehicleId()).ifPresent(v -> {
            if (v.getStatus() != VehicleStatus.MAINTENANCE) {
                v.setStatus(VehicleStatus.AVAILABLE);
            }
            vehicleRepository.save(v);
        });

        String eventType = wasActive ? "TRIP_CANCELLED" : "DRIVER_REJECTED";
        String message = wasActive
                ? "Tài xế đã hủy chuyến. Tiền sẽ được hoàn lại vào ví của bạn."
                : "Tài xế đã từ chối đơn thuê. Vui lòng đặt lại hoặc chọn xe khác.";
        bookingRealtimeService.publishRentalUpdate(rental, eventType, message);

        // Thông báo cho khách hàng
        try {
            Customer cust = customerRepository.findById(rental.getCustomerId()).orElse(null);
            if (cust != null) {
                notificationService.createNotification(
                        cust.getUserId(), "Đơn thuê xe bị hủy",
                        "Đơn thuê xe #" + rental.getId() + " đã bị tài xế hủy. " +
                        (rental.getPaymentStatus() == PaymentStatus.REFUNDED ? "Tiền đã được hoàn vào ví." : "") +
                        (reason != null && !reason.isBlank() ? " Lý do: " + reason : ""),
                        NotificationType.RENTAL_REJECTED,
                        NotificationRefType.RENTAL, rental.getId());
            }
        } catch (Exception ignored) {}
        return ServiceResult.success("Đã hủy đơn thuê xe." +
                (rental.getPaymentStatus() == PaymentStatus.REFUNDED ? " Tiền đã được hoàn lại cho khách." : ""),
                rental.getId());
    }

    /**
     * Khách hàng hủy đơn thuê xe (chỉ khi tài xế chưa bắt đầu chuyến).
     */
    public ServiceResult cancelByCustomer(Long rentalId, Long customerUserId) {
        Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            return ServiceResult.error("Không tìm thấy đơn thuê.");
        }
        VehicleRental rental = rentalOpt.get();

        // Kiểm tra quyền
        Optional<Customer> custOpt = customerRepository.findByUserId(customerUserId);
        if (custOpt.isEmpty() || !custOpt.get().getId().equals(rental.getCustomerId())) {
            return ServiceResult.error("Bạn không có quyền hủy đơn này.");
        }

        // Chỉ cho hủy khi PENDING hoặc CONFIRMED (chưa bắt đầu)
        if (rental.getStatus() != VehicleRental.RentalStatus.PENDING
            && rental.getStatus() != VehicleRental.RentalStatus.CONFIRMED) {
            return ServiceResult.error("Không thể hủy đơn đã bắt đầu hoặc đã hoàn thành.");
        }

        rental.setStatus(VehicleRental.RentalStatus.CANCELLED);
        rental.setCancelReason("Khách hàng tự hủy đơn.");
        vehicleRentalRepository.save(rental);

        // Giải phóng tài xế
        if (rental.getDriverId() != null) {
            driverRepository.findById(rental.getDriverId()).ifPresent(d -> {
                d.setIsAvailable(true);
                driverRepository.save(d);
            });
        }
        // Giải phóng xe
        vehicleRepository.findById(rental.getVehicleId()).ifPresent(v -> {
            if (v.getStatus() != VehicleStatus.MAINTENANCE) {
                v.setStatus(VehicleStatus.AVAILABLE);
            }
            vehicleRepository.save(v);
        });

        bookingRealtimeService.publishRentalUpdate(rental, "TRIP_CANCELLED", "Khách hàng đã hủy đơn thuê xe.");

        // Thông báo cho tài xế
        try {
            if (rental.getDriverId() != null) {
                Driver driver = driverRepository.findById(rental.getDriverId()).orElse(null);
                if (driver != null) {
                    notificationService.createNotification(
                            driver.getUserId(), "Khách hàng hủy đơn thuê",
                            "Đơn thuê xe #" + rental.getId() + " đã bị khách hàng hủy.",
                            NotificationType.RENTAL_REJECTED,
                            NotificationRefType.RENTAL, rental.getId());
                }
            }
        } catch (Exception ignored) {}

        return ServiceResult.success("Đã hủy đơn thuê xe thành công.", rental.getId());
    }

    public ServiceResult startTrip(Long rentalId, Long driverUserId) {
        Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay don thue.");
        }
        VehicleRental rental = rentalOpt.get();
        if (rental.getStatus() != VehicleRental.RentalStatus.CONFIRMED) {
            return ServiceResult.error("Don thue chua duoc xac nhan.");
        }

        Optional<Driver> actorDriver = driverRepository.findByUserId(driverUserId);
        if (actorDriver.isEmpty() || rental.getDriverId() == null || !actorDriver.get().getId().equals(rental.getDriverId())) {
            return ServiceResult.error("Ban khong co quyen bat dau chuyen.");
        }

        rental.setStatus(VehicleRental.RentalStatus.ACTIVE);
        rental.setActualStart(LocalDateTime.now());
        vehicleRentalRepository.save(rental);

        vehicleRepository.findById(rental.getVehicleId()).ifPresent(v -> {
            v.setStatus(VehicleStatus.ON_TRIP);
            vehicleRepository.save(v);
        });
        bookingRealtimeService.publishRentalUpdate(
                rental,
                "TRIP_STARTED",
                "Tai xe da bat dau chuyen."
        );
        // In-app notification for customer
        try {
            Customer cust = customerRepository.findById(rental.getCustomerId()).orElse(null);
            if (cust != null) {
                notificationService.createNotification(
                        cust.getUserId(), "Chuyến thuê xe bắt đầu",
                        "Đơn thuê xe #" + rental.getId() + " đã bắt đầu.",
                        NotificationType.TRIP_STARTED,
                        NotificationRefType.RENTAL, rental.getId());
            }
        } catch (Exception ignored) {}
        return ServiceResult.success("Da bat dau chuyen.", rental.getId());
    }

    public ServiceResult completeTrip(Long rentalId, Long driverUserId, BigDecimal extraFee, String notes) {
        Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay don thue.");
        }
        VehicleRental rental = rentalOpt.get();

        if (rental.getStatus() != VehicleRental.RentalStatus.ACTIVE) {
            return ServiceResult.error("Don thue chua bat dau hoac da ket thuc.");
        }
        if (driverUserId != null) {
            Optional<Driver> actorDriver = driverRepository.findByUserId(driverUserId);
            if (actorDriver.isEmpty() || rental.getDriverId() == null || !actorDriver.get().getId().equals(rental.getDriverId())) {
                return ServiceResult.error("Ban khong co quyen hoan thanh don nay.");
            }
        }

        BigDecimal safeExtraFee = extraFee == null ? BigDecimal.ZERO : extraFee;
        if (safeExtraFee.compareTo(BigDecimal.ZERO) < 0) {
            return ServiceResult.error("Phi phat sinh khong hop le.");
        }

        BigDecimal totalPrice = rental.getBasePrice()
                .add(safeExtraFee)
                .subtract(rental.getDiscountAmount() == null ? BigDecimal.ZERO : rental.getDiscountAmount());
        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO;
        }
        totalPrice = totalPrice.setScale(2, RoundingMode.HALF_UP);

        Driver driver = rental.getDriverId() == null ? null : driverRepository.findById(rental.getDriverId()).orElse(null);
        if (driver == null) {
            return ServiceResult.error("Khong the cap nhat don do thieu du lieu tai xe.");
        }

        rental.setExtraFee(safeExtraFee.setScale(2, RoundingMode.HALF_UP));
        rental.setTotalPrice(totalPrice);
        rental.setActualEnd(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            rental.setNotes(notes.trim());
        }
        rental.setStatus(VehicleRental.RentalStatus.COMPLETED);

        // ── Tự động thanh toán khi hoàn thành (trường hợp trả tiền mặt) ──
        if (rental.getPaymentStatus() != PaymentStatus.PAID) {
            rental.setPaymentStatus(PaymentStatus.PAID);

            // Chia tiền: 60% tài xế, 40% hệ thống
            if (totalPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal driverShare = totalPrice.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal systemShare = totalPrice.subtract(driverShare);

                walletService.credit(
                        driver.getUserId(),
                        driverShare,
                        TransactionType.DRIVER_EARNING,
                        ReferenceType.RENTAL,
                        rental.getId(),
                        "Thu nhap tai xe tu don thue #" + rental.getId()
                );
                userRepository.findFirstByRole(Role.ADMIN).ifPresent(admin ->
                        walletService.credit(
                                admin.getId(),
                                systemShare,
                                TransactionType.SYSTEM_FEE,
                                ReferenceType.RENTAL,
                                rental.getId(),
                                "He thong nhan phi tu don thue #" + rental.getId()
                        )
                );
            }
        }
        vehicleRentalRepository.save(rental);

        vehicleRepository.findById(rental.getVehicleId()).ifPresent(v -> {
            v.setStatus(VehicleStatus.AVAILABLE);
            v.setTotalTrips((v.getTotalTrips() == null ? 0 : v.getTotalTrips()) + 1);
            vehicleRepository.save(v);
        });

        driver.setIsAvailable(true);
        driverRepository.save(driver);

        bookingRealtimeService.publishRentalUpdate(
                rental,
                "TRIP_COMPLETED",
                "Chuyen di da hoan thanh. Da thanh toan."
        );
        // In-app notification cho customer
        try {
            Customer customer = customerRepository.findById(rental.getCustomerId()).orElse(null);
            if (customer != null) {
                notificationService.createNotification(
                        customer.getUserId(), "Chuyến đi hoàn thành",
                        "Chuyến thuê xe #" + rental.getId() + " đã hoàn thành và đã thanh toán.",
                        NotificationType.TRIP_COMPLETED,
                        NotificationRefType.RENTAL, rental.getId());
            }
        } catch (Exception ignored) {}
        return ServiceResult.success("Da hoan thanh chuyen va da thanh toan.", rental.getId());
    }

    public ServiceResult payRental(Long rentalId, Long customerUserId) {
        Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay don thue.");
        }
        VehicleRental rental = rentalOpt.get();

        Optional<Customer> actorOpt = customerRepository.findByUserId(customerUserId);
        if (actorOpt.isEmpty() || !actorOpt.get().getId().equals(rental.getCustomerId())) {
            return ServiceResult.error("Ban khong co quyen thanh toan don nay.");
        }
        if (rental.getStatus() == VehicleRental.RentalStatus.CANCELLED) {
            return ServiceResult.error("Don da huy, khong the thanh toan.");
        }
        if (rental.getStatus() != VehicleRental.RentalStatus.COMPLETED) {
            return ServiceResult.error("Don chua hoan thanh, khong the thanh toan.");
        }
        if (rental.getPaymentStatus() == PaymentStatus.PAID) {
            return ServiceResult.error("Don nay da thanh toan truoc do.");
        }

        Customer customer = actorOpt.get();
        Driver driver = driverRepository.findById(rental.getDriverId()).orElse(null);
        if (driver == null) {
            return ServiceResult.error("Khong tim thay thong tin tai xe.");
        }

        BigDecimal totalPrice = rental.getTotalPrice() == null ? BigDecimal.ZERO : rental.getTotalPrice();
        if (totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            rental.setPaymentStatus(PaymentStatus.PAID);
            vehicleRentalRepository.save(rental);
            return ServiceResult.success("Thanh toan thanh cong.", rental.getId());
        }

        BigDecimal driverShare = totalPrice.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal systemShare = totalPrice.subtract(driverShare);

        String debitErr = walletService.debit(
                customer.getUserId(),
                totalPrice,
                TransactionType.RENTAL_PAYMENT,
                ReferenceType.RENTAL,
                rental.getId(),
                "Khach thanh toan don thue #" + rental.getId()
        );
        if (debitErr != null) {
            return ServiceResult.error(debitErr);
        }

        walletService.credit(
                driver.getUserId(),
                driverShare,
                TransactionType.DRIVER_EARNING,
                ReferenceType.RENTAL,
                rental.getId(),
                "Thu nhap tai xe tu don thue #" + rental.getId()
        );
        userRepository.findFirstByRole(Role.ADMIN).ifPresent(admin ->
                walletService.credit(
                        admin.getId(),
                        systemShare,
                        TransactionType.SYSTEM_FEE,
                        ReferenceType.RENTAL,
                        rental.getId(),
                        "He thong nhan phi tu don thue #" + rental.getId()
                )
        );

        rental.setPaymentStatus(PaymentStatus.PAID);
        vehicleRentalRepository.save(rental);
        // Notifications: customer + driver
        try {
            notificationService.createNotification(
                    customerUserId, "Thanh toán thành công",
                    "Đơn thuê xe #" + rental.getId() + " đã thanh toán thành công.",
                    NotificationType.RENTAL_PAID,
                    NotificationRefType.RENTAL, rental.getId());
            notificationService.createNotification(
                    driver.getUserId(), "Nhận tiền đơn thuê xe",
                    "Bạn đã nhận tiền cho đơn thuê xe #" + rental.getId() + ".",
                    NotificationType.PAYMENT_DONE,
                    NotificationRefType.RENTAL, rental.getId());
        } catch (Exception ignored) {}
        return ServiceResult.success("Thanh toan thanh cong. Da tru vi khach hang.", rental.getId());
    }

    public ServiceResult cancelRental(Long rentalId, Long customerUserId, String cancelReason) {
        Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay don thue.");
        }
        VehicleRental rental = rentalOpt.get();

        Optional<Customer> actorOpt = customerRepository.findByUserId(customerUserId);
        if (actorOpt.isEmpty() || !actorOpt.get().getId().equals(rental.getCustomerId())) {
            return ServiceResult.error("Ban khong co quyen huy don nay.");
        }
        if (rental.getStatus() == VehicleRental.RentalStatus.CANCELLED) {
            return ServiceResult.error("Don nay da duoc huy truoc do.");
        }
        if (rental.getStatus() == VehicleRental.RentalStatus.COMPLETED) {
            return ServiceResult.error("Don da hoan thanh, khong the huy.");
        }
        if (rental.getStatus() == VehicleRental.RentalStatus.ACTIVE) {
            return ServiceResult.error("Don dang chay, khong the huy.");
        }
        if (rental.getPaymentStatus() == PaymentStatus.PAID) {
            return ServiceResult.error("Don da thanh toan, khong the huy.");
        }

        rental.setStatus(VehicleRental.RentalStatus.CANCELLED);
        rental.setCancelReason((cancelReason == null || cancelReason.isBlank()) ? "Khach hang yeu cau huy don." : cancelReason.trim());
        vehicleRentalRepository.save(rental);

        vehicleRepository.findById(rental.getVehicleId()).ifPresent(v -> {
            if (v.getStatus() != VehicleStatus.MAINTENANCE) {
                v.setStatus(VehicleStatus.AVAILABLE);
            }
            vehicleRepository.save(v);
        });
        if (rental.getDriverId() != null) {
            driverRepository.findById(rental.getDriverId()).ifPresent(d -> {
                d.setIsAvailable(true);
                driverRepository.save(d);
            });
        }
        bookingRealtimeService.publishRentalUpdate(
                rental,
                "TRIP_CANCELLED",
                "Don cua ban da duoc huy."
        );
        // In-app notification for customer + driver
        try {
            notificationService.createNotification(
                    customerUserId, "Đã hủy đơn thuê xe",
                    "Đơn thuê xe #" + rental.getId() + " đã hủy thành công.",
                    NotificationType.RENTAL_CANCELLED,
                    NotificationRefType.RENTAL, rental.getId());
            if (rental.getDriverId() != null) {
                driverRepository.findById(rental.getDriverId()).ifPresent(d ->
                    notificationService.createNotification(
                            d.getUserId(), "Khách hủy đơn thuê xe",
                            "Đơn thuê xe #" + rental.getId() + " đã bị khách hàng hủy.",
                            NotificationType.RENTAL_CANCELLED,
                            NotificationRefType.RENTAL, rental.getId())
                );
            }
        } catch (Exception ignored) {}
        return ServiceResult.success("Huy don thanh cong.", rental.getId());
    }

    private Optional<Driver> getAssignedDriverForVehicle(
            Vehicle vehicle,
            LocalDateTime plannedStart,
            LocalDateTime plannedEnd,
            List<VehicleRental.RentalStatus> busyStatuses) {
        if (vehicle.getAssignedDriver() == null) {
            return Optional.empty();
        }
        Optional<Driver> assignedOpt = driverRepository.findById(vehicle.getAssignedDriver());
        if (assignedOpt.isEmpty()) {
            return Optional.empty();
        }
        Driver assigned = assignedOpt.get();
        if (!isDriverEligible(assigned, vehicle.getCategory())) {
            return Optional.empty();
        }
        if (vehicleRentalRepository.hasDriverOverlap(assigned.getId(), plannedStart, plannedEnd, busyStatuses)) {
            return Optional.empty();
        }
        return Optional.of(assigned);
    }

    private boolean isDriverEligible(Driver driver, VehicleCategory category) {
        if (category == null) {
            return false;
        }
        return driver.getVerificationStatus() == VerificationStatus.APPROVED
                && Boolean.TRUE.equals(driver.getIsAvailable())
                && supportsCategory(driver.getVehicleTypes(), category);
    }

    private boolean supportsCategory(String vehicleTypes, VehicleCategory category) {
        if (vehicleTypes == null || vehicleTypes.isBlank()) {
            return false;
        }
        String normalized = "," + vehicleTypes.toUpperCase().replace(" ", "") + ",";
        return normalized.contains("," + category.name() + ",");
    }

    private BigDecimal calculateBasePrice(
            Vehicle vehicle,
            VehicleRental.RentalType rentalType,
            LocalDateTime plannedStart,
            LocalDateTime plannedEnd) {
        long minutes = Duration.between(plannedStart, plannedEnd).toMinutes();
        if (rentalType == VehicleRental.RentalType.DAILY) {
            long days = (long) Math.ceil(minutes / 1440d);
            days = Math.max(1, days);
            return vehicle.getPricePerDay().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        }

        long hours = (long) Math.ceil(minutes / 60d);
        hours = Math.max(1, hours);
        return vehicle.getPricePerHour().multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
    }

    public record ServiceResult(boolean ok, String message, Long rentalId) {
        public static ServiceResult success(String message, Long rentalId) {
            return new ServiceResult(true, message, rentalId);
        }

        public static ServiceResult error(String message) {
            return new ServiceResult(false, message, null);
        }
    }
}
