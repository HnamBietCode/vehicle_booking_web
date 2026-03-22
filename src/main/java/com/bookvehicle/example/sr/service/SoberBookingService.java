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
    
    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private BookingRealtimeService bookingRealtimeService;

    @Autowired
    private GeocodingService geocodingService;


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

        // Use form-selected coordinates first, then fallback to geocoding.
        if (booking.getPickupLat() == null || booking.getPickupLng() == null) {
            try {
                GeocodingService.LatLng coords = geocodingService.geocode(
                        booking.getPickupAddress(),
                        booking.getWard(),
                        booking.getDistrict(),
                        booking.getProvince()
                );
                if (coords != null) {
                    booking.setPickupLat(coords.lat());
                    booking.setPickupLng(coords.lng());
                }
            } catch (Exception ignored) {
            }
        }

        // No auto-assign: tai xe se tu nhan don
        notifyAvailableDrivers(booking);
        return soberBookingRepository.save(booking);
    }

    private void notifyAvailableDrivers(SoberBooking booking) {
        try {
            List<Driver> drivers = driverRepository.findAvailableByVehicleType(booking.getVehicleCategory().name());
            String province = booking.getProvince() != null ? booking.getProvince().trim() : "";
            if (!province.isBlank()) {
                drivers = drivers.stream()
                        .filter(d -> province.equalsIgnoreCase(d.getProvince()))
                        .toList();
            }
            List<Long> userIds = drivers.stream().map(Driver::getUserId).toList();
            pushNotificationService.notifyUsers(
                    userIds,
                    "Co don thue tai xe moi",
                    "Khach hang dang can tai xe. Bam de nhan don.",
                    java.util.Map.of("tripType", "SOBER", "tripId", String.valueOf(booking.getId()))
            );
        } catch (Exception ignored) {
        }
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
        
        SoberBooking saved = soberBookingRepository.save(booking);
        bookingRealtimeService.publishSoberUpdate(
                saved,
                "DRIVER_ACCEPTED",
                "Tai xe da nhan don cua ban. Ban co the mo trang theo doi de doi vi tri."
        );
        return saved;
    }

    @Transactional
    public SoberBooking arriveAtPickup(Long bookingId, Long driverUserId) {
        SoberBooking booking = getAndVerifyDriver(bookingId, driverUserId);

        if (booking.getStatus() != SoberBooking.SoberBookingStatus.ACCEPTED) {
            throw new RuntimeException("Invalid status transition: Must be ACCEPTED to arrive");
        }

        booking.setStatus(SoberBooking.SoberBookingStatus.ARRIVED);
        SoberBooking saved = soberBookingRepository.save(booking);
        bookingRealtimeService.publishSoberUpdate(
                saved,
                "DRIVER_ARRIVED",
                "Tai xe da toi diem don."
        );
        return saved;
    }

    @Transactional
    public SoberBooking startTrip(Long bookingId, Long driverUserId) {
        SoberBooking booking = getAndVerifyDriver(bookingId, driverUserId);

        if (booking.getStatus() != SoberBooking.SoberBookingStatus.ARRIVED) {
            throw new RuntimeException("Invalid status transition: Must be ARRIVED to start");
        }

        booking.setStatus(SoberBooking.SoberBookingStatus.IN_PROGRESS);
        booking.setActualStart(LocalDateTime.now());
        SoberBooking saved = soberBookingRepository.save(booking);
        bookingRealtimeService.publishSoberUpdate(
                saved,
                "TRIP_STARTED",
                "Chuyen di da bat dau."
        );
        return saved;
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
        
        SoberBooking saved = soberBookingRepository.save(booking);
        bookingRealtimeService.publishSoberUpdate(
                saved,
                "TRIP_COMPLETED",
                "Chuyen di da hoan thanh."
        );
        return saved;
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

        SoberBooking saved = soberBookingRepository.save(booking);
        bookingRealtimeService.publishSoberUpdate(
                saved,
                "TRIP_CANCELLED",
                "Tai xe da huy don. Vui long dat lai hoac lien he ho tro."
        );
        return saved;
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

        SoberBooking saved = soberBookingRepository.save(booking);
        bookingRealtimeService.publishSoberUpdate(
                saved,
                "TRIP_CANCELLED",
                "Don cua ban da duoc huy."
        );
        return saved;
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
