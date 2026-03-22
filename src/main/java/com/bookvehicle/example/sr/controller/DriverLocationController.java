package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.DriverLocationRequest;
import com.bookvehicle.example.sr.dto.DriverLocationResponse;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.CustomerRepository;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import com.bookvehicle.example.sr.repository.VehicleRentalRepository;
import com.bookvehicle.example.sr.service.BookingRealtimeService;
import com.bookvehicle.example.sr.service.DriverLocationService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
public class DriverLocationController {

    private final DriverRepository driverRepository;
    private final SoberBookingRepository soberBookingRepository;
    private final VehicleRentalRepository vehicleRentalRepository;
    private final CustomerRepository customerRepository;
    private final DriverLocationService driverLocationService;
    private final BookingRealtimeService bookingRealtimeService;
    private final SimpMessagingTemplate messagingTemplate;

    public DriverLocationController(
            DriverRepository driverRepository,
            SoberBookingRepository soberBookingRepository,
            VehicleRentalRepository vehicleRentalRepository,
            CustomerRepository customerRepository,
            DriverLocationService driverLocationService,
            BookingRealtimeService bookingRealtimeService,
            SimpMessagingTemplate messagingTemplate) {
        this.driverRepository = driverRepository;
        this.soberBookingRepository = soberBookingRepository;
        this.vehicleRentalRepository = vehicleRentalRepository;
        this.customerRepository = customerRepository;
        this.driverLocationService = driverLocationService;
        this.bookingRealtimeService = bookingRealtimeService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/api/driver/locations")
    public ResponseEntity<?> updateLocation(@RequestBody DriverLocationRequest req, HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        if (req.getTripType() == null || req.getTripId() == null || req.getLat() == null || req.getLng() == null) {
            return ResponseEntity.badRequest().body("Du lieu khong hop le.");
        }

        Optional<Driver> driverOpt = driverRepository.findByUserId(loggedUser.getId());
        if (driverOpt.isEmpty()) {
            return ResponseEntity.status(403).build();
        }
        Driver driver = driverOpt.get();

        String tripType = req.getTripType().toUpperCase();
        if ("SOBER".equals(tripType)) {
            Optional<SoberBooking> bookingOpt = soberBookingRepository.findById(req.getTripId());
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            SoberBooking booking = bookingOpt.get();
            if (booking.getDriverId() == null || !booking.getDriverId().equals(driver.getId())) {
                return ResponseEntity.status(403).build();
            }
            if (booking.getStatus() != SoberBooking.SoberBookingStatus.ACCEPTED &&
                booking.getStatus() != SoberBooking.SoberBookingStatus.ARRIVED &&
                booking.getStatus() != SoberBooking.SoberBookingStatus.IN_PROGRESS) {
                return ResponseEntity.badRequest().body("Trang thai khong cho phep cap nhat vi tri.");
            }
        } else if ("RENTAL".equals(tripType)) {
            Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(req.getTripId());
            if (rentalOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            VehicleRental rental = rentalOpt.get();
            if (rental.getDriverId() == null || !rental.getDriverId().equals(driver.getId())) {
                return ResponseEntity.status(403).build();
            }
            if (rental.getStatus() != VehicleRental.RentalStatus.CONFIRMED &&
                rental.getStatus() != VehicleRental.RentalStatus.ACTIVE) {
                return ResponseEntity.badRequest().body("Trang thai khong cho phep cap nhat vi tri.");
            }
        } else {
            return ResponseEntity.badRequest().body("TripType khong ho tro.");
        }

        DriverLocationResponse payload = new DriverLocationResponse();
        payload.setTripType(tripType);
        payload.setTripId(req.getTripId());
        payload.setLat(req.getLat());
        payload.setLng(req.getLng());
        payload.setSpeed(req.getSpeed());
        payload.setBearing(req.getBearing());
        payload.setAccuracy(req.getAccuracy());
        payload.setRecordedAt(req.getRecordedAt() != null ? req.getRecordedAt() : Instant.now());

        boolean firstLocation = driverLocationService.getLocation(tripType, req.getTripId()) == null;
        driverLocationService.saveLocation(tripType, req.getTripId(), payload);
        messagingTemplate.convertAndSend("/topic/trips/" + tripType + "/" + req.getTripId(), payload);
        if (firstLocation) {
            bookingRealtimeService.publishLocationAvailable(tripType, req.getTripId());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/trips/{tripType}/{tripId}/driver-location")
    public ResponseEntity<DriverLocationResponse> getLocation(
            @PathVariable String tripType,
            @PathVariable Long tripId,
            HttpSession session,
            HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.CUSTOMER) {
            return ResponseEntity.status(401).build();
        }

        Optional<Customer> customerOpt = customerRepository.findByUserId(loggedUser.getId());
        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(403).build();
        }
        Long customerId = customerOpt.get().getId();

        String t = tripType.toUpperCase();
        if ("SOBER".equals(t)) {
            Optional<SoberBooking> bookingOpt = soberBookingRepository.findById(tripId);
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            if (!bookingOpt.get().getCustomerId().equals(customerId)) {
                return ResponseEntity.status(403).build();
            }
        } else if ("RENTAL".equals(t)) {
            Optional<VehicleRental> rentalOpt = vehicleRentalRepository.findById(tripId);
            if (rentalOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            if (!rentalOpt.get().getCustomerId().equals(customerId)) {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.badRequest().build();
        }

        DriverLocationResponse payload = driverLocationService.getLocation(t, tripId);
        if (payload == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(payload);
    }
}
