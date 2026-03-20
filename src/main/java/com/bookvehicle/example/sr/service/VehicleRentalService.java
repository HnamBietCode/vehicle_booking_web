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

    public VehicleRentalService(
            VehicleRentalRepository vehicleRentalRepository,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            PickupPointRepository pickupPointRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            WalletService walletService) {
        this.vehicleRentalRepository = vehicleRentalRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.pickupPointRepository = pickupPointRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
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

    public ServiceResult createRental(Long customerUserId, RentalCreateForm form) {
        Optional<Customer> customerOpt = customerRepository.findByUserId(customerUserId);
        if (customerOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay thong tin khach hang.");
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

        Optional<Driver> assignedDriver = pickDriverForVehicle(vehicle, form.getPlannedStart(), form.getPlannedEnd(), busyStatuses);
        if (assignedDriver.isEmpty()) {
            return ServiceResult.error("Khong tim thay tai xe phu hop cho lich nay.");
        }

        Driver driver = assignedDriver.get();
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

        VehicleRental rental = new VehicleRental();
        rental.setCustomerId(customerOpt.get().getId());
        rental.setVehicleId(vehicle.getId());
        rental.setDriverId(driver.getId());
        rental.setPickupPointId(pickupPointId);
        rental.setPickupAddress(pickupAddress);
        rental.setRentalType(form.getRentalType());
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
        return ServiceResult.success("Dat xe thanh cong. Cho tai xe xac nhan.", rental.getId());
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
        if (actorDriver.isEmpty() || !actorDriver.get().getId().equals(rental.getDriverId())) {
            return ServiceResult.error("Ban khong co quyen nhan don nay.");
        }

        Driver driver = actorDriver.get();
        if (!Boolean.TRUE.equals(driver.getIsAvailable())) {
            return ServiceResult.error("Tai xe dang ban, khong the nhan don.");
        }

        rental.setStatus(VehicleRental.RentalStatus.CONFIRMED);
        vehicleRentalRepository.save(rental);

        driver.setIsAvailable(false);
        driverRepository.save(driver);
        return ServiceResult.success("Da nhan don thue.", rental.getId());
    }

    public ServiceResult rejectRental(Long rentalId, Long driverUserId, String reason) {
        Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            return ServiceResult.error("Khong tim thay don thue.");
        }
        VehicleRental rental = rentalOpt.get();
        if (rental.getStatus() != VehicleRental.RentalStatus.PENDING) {
            return ServiceResult.error("Don thue khong o trang thai cho xac nhan.");
        }

        Optional<Driver> actorDriver = driverRepository.findByUserId(driverUserId);
        if (actorDriver.isEmpty() || !actorDriver.get().getId().equals(rental.getDriverId())) {
            return ServiceResult.error("Ban khong co quyen tu choi don nay.");
        }

        rental.setStatus(VehicleRental.RentalStatus.CANCELLED);
        rental.setCancelReason((reason == null || reason.isBlank()) ? "Tai xe tu choi nhan don." : reason.trim());
        vehicleRentalRepository.save(rental);

        driverRepository.findById(rental.getDriverId()).ifPresent(d -> {
            d.setIsAvailable(true);
            driverRepository.save(d);
        });
        vehicleRepository.findById(rental.getVehicleId()).ifPresent(v -> {
            if (v.getStatus() != VehicleStatus.MAINTENANCE) {
                v.setStatus(VehicleStatus.AVAILABLE);
            }
            vehicleRepository.save(v);
        });

        return ServiceResult.success("Da tu choi don thue.", rental.getId());
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
        if (actorDriver.isEmpty() || !actorDriver.get().getId().equals(rental.getDriverId())) {
            return ServiceResult.error("Ban khong co quyen bat dau chuyen.");
        }

        rental.setStatus(VehicleRental.RentalStatus.ACTIVE);
        rental.setActualStart(LocalDateTime.now());
        vehicleRentalRepository.save(rental);

        vehicleRepository.findById(rental.getVehicleId()).ifPresent(v -> {
            v.setStatus(VehicleStatus.ON_TRIP);
            vehicleRepository.save(v);
        });
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
            if (actorDriver.isEmpty() || !actorDriver.get().getId().equals(rental.getDriverId())) {
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

        Driver driver = driverRepository.findById(rental.getDriverId()).orElse(null);
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
        if (rental.getPaymentStatus() != PaymentStatus.PAID) {
            rental.setPaymentStatus(PaymentStatus.PENDING);
        }
        vehicleRentalRepository.save(rental);

        vehicleRepository.findById(rental.getVehicleId()).ifPresent(v -> {
            v.setStatus(VehicleStatus.AVAILABLE);
            v.setTotalTrips((v.getTotalTrips() == null ? 0 : v.getTotalTrips()) + 1);
            vehicleRepository.save(v);
        });

        driver.setIsAvailable(true);
        driverRepository.save(driver);

        return ServiceResult.success("Da hoan thanh chuyen. Cho khach hang thanh toan.", rental.getId());
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
        driverRepository.findById(rental.getDriverId()).ifPresent(d -> {
            d.setIsAvailable(true);
            driverRepository.save(d);
        });

        return ServiceResult.success("Huy don thanh cong.", rental.getId());
    }

    private Optional<Driver> pickDriverForVehicle(
            Vehicle vehicle,
            LocalDateTime plannedStart,
            LocalDateTime plannedEnd,
            List<VehicleRental.RentalStatus> busyStatuses) {
        if (vehicle.getAssignedDriver() != null) {
            Optional<Driver> assignedOpt = driverRepository.findById(vehicle.getAssignedDriver());
            if (assignedOpt.isPresent()) {
                Driver assigned = assignedOpt.get();
                if (isDriverEligible(assigned, vehicle.getCategory()) &&
                        !vehicleRentalRepository.hasDriverOverlap(assigned.getId(), plannedStart, plannedEnd, busyStatuses)) {
                    return Optional.of(assigned);
                }
            }
        }

        return driverRepository.findAll().stream()
                .filter(d -> isDriverEligible(d, vehicle.getCategory()))
                .filter(d -> !vehicleRentalRepository.hasDriverOverlap(d.getId(), plannedStart, plannedEnd, busyStatuses))
                .findFirst();
    }

    private boolean isDriverEligible(Driver driver, VehicleCategory category) {
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
