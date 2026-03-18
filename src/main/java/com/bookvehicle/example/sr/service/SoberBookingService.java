package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SoberBookingService {

    @Autowired
    private SoberBookingRepository soberBookingRepository;

    @Autowired
    private SoberRateRepository soberRateRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;


    @Transactional
    public SoberBooking createBooking(Long customerUserId, SoberBooking booking) {
        Customer customer = customerRepository.findByUserId(customerUserId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Constraint: Only one active booking at a time
        long activeCount = soberBookingRepository.countByCustomerIdAndStatusNotIn(
                customer.getId(), 
                List.of(SoberBooking.SoberBookingStatus.COMPLETED, SoberBooking.SoberBookingStatus.CANCELLED)
        );
        if (activeCount > 0) {
            throw new RuntimeException("Bạn đang có một đơn thuê tài xế chưa hoàn thành. Vui lòng kết thúc hoặc hủy đơn cũ trước khi đặt đơn mới.");
        }

        // Constraint: Cannot book if there is an unpaid completed booking
        long unpaidCount = soberBookingRepository.countUnpaidBookings(customer.getId());
        if (unpaidCount > 0) {
            throw new RuntimeException("Bạn có đơn hàng chưa thanh toán. Vui lòng thanh toán các đơn cũ trước khi đặt đơn mới.");
        }

        booking.setCustomerId(customer.getId());
        BigDecimal rate = calculateRate(booking);
        if (booking.getDurationUnit() == SoberBooking.DurationUnit.HOURLY) {
            booking.setHourlyRate(rate);
        } else {
            booking.setDailyRate(rate);
        }
        booking.setTotalPrice(rate.multiply(new BigDecimal(booking.getDuration())));
        booking.setStatus(SoberBooking.SoberBookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);

        // Advanced Driver Selection
        List<Driver> availableDrivers = driverRepository.findAvailableByVehicleType(booking.getVehicleCategory().name());
        
        // Strict Constraint: Must be in the same province
        final String province = booking.getProvince() != null ? booking.getProvince() : "";
        availableDrivers = availableDrivers.stream()
                .filter(d -> province.equalsIgnoreCase(d.getProvince()))
                .collect(java.util.stream.Collectors.toList());

        if (!availableDrivers.isEmpty()) {
            final String ward = booking.getWard() != null ? booking.getWard() : "";
            final String district = booking.getDistrict() != null ? booking.getDistrict() : "";
            // province variable already declared above

            availableDrivers.sort((d1, d2) -> {
                // 1. Ward Match
                boolean d1Ward = ward.equalsIgnoreCase(d1.getWard());
                boolean d2Ward = ward.equalsIgnoreCase(d2.getWard());
                if (d1Ward != d2Ward) return d1Ward ? -1 : 1;

                // 2. District Match
                boolean d1Dist = district.equalsIgnoreCase(d1.getDistrict());
                boolean d2Dist = district.equalsIgnoreCase(d2.getDistrict());
                if (d1Dist != d2Dist) return d1Dist ? -1 : 1;

                // 3. Province Match
                boolean d1Prov = province.equalsIgnoreCase(d1.getProvince());
                boolean d2Prov = province.equalsIgnoreCase(d2.getProvince());
                if (d1Prov != d2Prov) return d1Prov ? -1 : 1;

                // 4. Last Completed At (earliest first)
                if (d1.getLastCompletedAt() == null && d2.getLastCompletedAt() == null) return 0;
                if (d1.getLastCompletedAt() == null) return -1;
                if (d2.getLastCompletedAt() == null) return 1;
                return d1.getLastCompletedAt().compareTo(d2.getLastCompletedAt());
            });

            Driver assignedDriver = availableDrivers.get(0);
            
            // Re-verify availability to prevent race conditions
            Driver latestDriverState = driverRepository.findById(assignedDriver.getId()).orElse(null);
            if (latestDriverState != null && Boolean.TRUE.equals(latestDriverState.getIsAvailable())) {
                booking.setDriverId(assignedDriver.getId());
                booking.setStatus(SoberBooking.SoberBookingStatus.ACCEPTED);
                
                latestDriverState.setIsAvailable(false);
                driverRepository.save(latestDriverState);
            } else {
                // Driver became unavailable in the split second, booking stays PENDING
                booking.setStatus(SoberBooking.SoberBookingStatus.PENDING);
            }
        }

        return soberBookingRepository.save(booking);
    }

    private BigDecimal calculateRate(SoberBooking booking) {
        SoberRate rateObj = soberRateRepository.findByVehicleCategory(booking.getVehicleCategory())
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình giá cho loại xe: " + booking.getVehicleCategory()));
        
        booking.setHourlyRate(rateObj.getHourlyRate());
        booking.setDailyRate(rateObj.getDailyRate());

        return (booking.getDurationUnit() == SoberBooking.DurationUnit.HOURLY) 
                ? rateObj.getHourlyRate() 
                : rateObj.getDailyRate();
    }

    @Transactional
    public SoberBooking acceptBooking(Long bookingId, Long driverUserId) {
        SoberBooking booking = soberBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != SoberBooking.SoberBookingStatus.PENDING) {
            throw new RuntimeException("Booking is no longer available");
        }

        Driver driver = driverRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (!Boolean.TRUE.equals(driver.getIsAvailable())) {
            throw new RuntimeException("Driver is not online/available");
        }

        booking.setDriverId(driver.getId());
        booking.setStatus(SoberBooking.SoberBookingStatus.ACCEPTED);
        
        // Mark driver as busy
        driver.setIsAvailable(false);
        driverRepository.save(driver);
        
        return soberBookingRepository.save(booking);
    }

    @Transactional
    public SoberBooking arriveAtPickup(Long bookingId, Long driverUserId) {
        SoberBooking booking = getAndVerifyDriver(bookingId, driverUserId);

        if (booking.getStatus() != SoberBooking.SoberBookingStatus.ACCEPTED) {
            throw new RuntimeException("Invalid status transition: Must be ACCEPTED to arrive");
        }

        booking.setStatus(SoberBooking.SoberBookingStatus.ARRIVED);
        return soberBookingRepository.save(booking);
    }

    @Transactional
    public SoberBooking startTrip(Long bookingId, Long driverUserId) {
        SoberBooking booking = getAndVerifyDriver(bookingId, driverUserId);

        if (booking.getStatus() != SoberBooking.SoberBookingStatus.ARRIVED) {
            throw new RuntimeException("Invalid status transition: Must be ARRIVED to start");
        }

        booking.setStatus(SoberBooking.SoberBookingStatus.IN_PROGRESS);
        booking.setActualStart(LocalDateTime.now());
        return soberBookingRepository.save(booking);
    }

    @Transactional
    public SoberBooking completeTrip(Long bookingId, Long driverUserId) {
        SoberBooking booking = getAndVerifyDriver(bookingId, driverUserId);

        if (booking.getStatus() != SoberBooking.SoberBookingStatus.IN_PROGRESS) {
            throw new RuntimeException("Invalid status transition: Must be IN_PROGRESS to complete");
        }

        booking.setStatus(SoberBooking.SoberBookingStatus.COMPLETED);
        booking.setActualEnd(LocalDateTime.now());
        
        // Make driver available again
        Driver driver = driverRepository.findById(booking.getDriverId()).orElse(null);
        if (driver != null) {
            driver.setIsAvailable(true);
            driver.setLastCompletedAt(LocalDateTime.now());
            driverRepository.save(driver);
        }
        
        return soberBookingRepository.save(booking);
    }

    @Transactional
    public SoberBooking payBooking(Long bookingId, Long customerUserId) {
        SoberBooking booking = soberBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Customer customer = customerRepository.findByUserId(customerUserId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (!customer.getId().equals(booking.getCustomerId())) {
            throw new RuntimeException("Unauthorized: Not your booking");
        }

        if (booking.getStatus() != SoberBooking.SoberBookingStatus.COMPLETED) {
            throw new RuntimeException("Booking is not completed yet");
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Booking is already paid");
        }

        BigDecimal totalPrice = booking.getTotalPrice();
        if (totalPrice.compareTo(BigDecimal.ZERO) > 0) {
            // Split: 80% driver, 20% system
            BigDecimal driverShare = totalPrice.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal systemShare = totalPrice.subtract(driverShare);

            String error = walletService.debit(
                    customerUserId,
                    totalPrice,
                    TransactionType.BOOKING_PAYMENT,
                    ReferenceType.BOOKING,
                    bookingId,
                    "Pay for sober driver booking #" + bookingId
            );

            if (error != null) {
                throw new RuntimeException(error);
            }

            // Pay driver
            Driver driver = driverRepository.findById(booking.getDriverId()).orElse(null);
            if (driver != null) {
                walletService.credit(
                        driver.getUserId(),
                        driverShare,
                        TransactionType.DRIVER_EARNING,
                        ReferenceType.BOOKING,
                        bookingId,
                        "Driver earning from sober booking #" + bookingId
                );
            }

            // Pay admin
            userRepository.findFirstByRole(Role.ADMIN).ifPresent(admin -> 
                walletService.credit(
                    admin.getId(),
                    systemShare,
                    TransactionType.SYSTEM_FEE,
                    ReferenceType.BOOKING,
                    bookingId,
                    "System fee from sober booking #" + bookingId
                )
            );
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        return soberBookingRepository.save(booking);
    }

    @Transactional
    public SoberBooking cancelByDriver(Long bookingId, Long driverUserId, String reason) {
        SoberBooking booking = getAndVerifyDriver(bookingId, driverUserId);
        
        if (booking.getStatus() == SoberBooking.SoberBookingStatus.COMPLETED || 
            booking.getStatus() == SoberBooking.SoberBookingStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng này đã kết thúc hoặc đã bị hủy.");
        }

        booking.setStatus(SoberBooking.SoberBookingStatus.CANCELLED);
        booking.setCancelReason(reason);

        // Make driver available again
        Driver driver = driverRepository.findById(booking.getDriverId()).orElse(null);
        if (driver != null) {
            driver.setIsAvailable(true);
            driver.setLastCompletedAt(LocalDateTime.now());
            driverRepository.save(driver);
        }

        return soberBookingRepository.save(booking);
    }

    @Transactional
    public SoberBooking cancelByCustomer(Long bookingId, Long customerUserId) {
        SoberBooking booking = soberBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Customer customer = customerRepository.findByUserId(customerUserId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (!customer.getId().equals(booking.getCustomerId())) {
            throw new RuntimeException("Unauthorized: Not your booking");
        }

        if (booking.getStatus() != SoberBooking.SoberBookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy đơn khi đang chờ tài xế.");
        }

        booking.setStatus(SoberBooking.SoberBookingStatus.CANCELLED);
        booking.setCancelReason("Khách hàng chủ động hủy đơn trước khi tài xế nhận.");

        return soberBookingRepository.save(booking);
    }

    private SoberBooking getAndVerifyDriver(Long bookingId, Long driverUserId) {
        SoberBooking booking = soberBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Driver driver = driverRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (!driver.getId().equals(booking.getDriverId())) {
            throw new RuntimeException("Unauthorized: Not your assigned booking");
        }
        return booking;
    }

    public List<SoberBooking> getPendingBookings() {
        return soberBookingRepository.findPendingBookings();
    }

    public SoberBooking getById(Long id) {
        return soberBookingRepository.findById(id).orElse(null);
    }

    @Transactional
    public void autoCancelTimedOutBookings() {
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusMinutes(15);
        List<SoberBooking> timedOut = soberBookingRepository.findByStatusAndCreatedAtBefore(
                SoberBooking.SoberBookingStatus.PENDING, 
                threshold
        );
        for (SoberBooking b : timedOut) {
            b.setStatus(SoberBooking.SoberBookingStatus.CANCELLED);
            b.setCancelReason("Không tìm thấy tài xế sau 15 phút. Vui lòng đặt đơn mới hoặc chờ ít phút nữa.");
            soberBookingRepository.save(b);
        }
    }
}
