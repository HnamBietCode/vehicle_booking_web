package com.bookvehicle.example.sr.controller.driver;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.DriverActiveTripResponse;
import com.bookvehicle.example.sr.model.Driver;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.SoberBooking;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.VehicleRental;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import com.bookvehicle.example.sr.repository.VehicleRentalRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/driver/trips")
public class DriverTripRestController {

    private final DriverRepository driverRepository;
    private final VehicleRentalRepository vehicleRentalRepository;
    private final SoberBookingRepository soberBookingRepository;

    public DriverTripRestController(DriverRepository driverRepository,
                                    VehicleRentalRepository vehicleRentalRepository,
                                    SoberBookingRepository soberBookingRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRentalRepository = vehicleRentalRepository;
        this.soberBookingRepository = soberBookingRepository;
    }

    @GetMapping("/active")
    public ResponseEntity<DriverActiveTripResponse> active(HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }

        Optional<Driver> driverOpt = driverRepository.findByUserId(loggedUser.getId());
        if (driverOpt.isEmpty()) {
            return ResponseEntity.ok(DriverActiveTripResponse.empty());
        }

        Long driverId = driverOpt.get().getId();

        Optional<DriverActiveTripResponse> soberTrip = soberBookingRepository.findByDriverId(driverId).stream()
                .filter(this::isTrackableSoberStatus)
                .sorted(Comparator
                        .comparingInt((SoberBooking booking) -> soberPriority(booking.getStatus()))
                        .thenComparing(SoberBooking::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSoberResponse)
                .findFirst();
        if (soberTrip.isPresent()) {
            return ResponseEntity.ok(soberTrip.get());
        }

        Optional<DriverActiveTripResponse> rentalTrip = vehicleRentalRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream()
                .filter(this::isTrackableRentalStatus)
                .sorted(Comparator
                        .comparingInt((VehicleRental rental) -> rentalPriority(rental.getStatus()))
                        .thenComparing(VehicleRental::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toRentalResponse)
                .findFirst();
        return ResponseEntity.ok(rentalTrip.orElseGet(DriverActiveTripResponse::empty));
    }

    private boolean isTrackableSoberStatus(SoberBooking booking) {
        return List.of(
                SoberBooking.SoberBookingStatus.ACCEPTED,
                SoberBooking.SoberBookingStatus.ARRIVED,
                SoberBooking.SoberBookingStatus.IN_PROGRESS
        ).contains(booking.getStatus());
    }

    private boolean isTrackableRentalStatus(VehicleRental rental) {
        return List.of(
                VehicleRental.RentalStatus.CONFIRMED,
                VehicleRental.RentalStatus.ACTIVE
        ).contains(rental.getStatus());
    }

    private int soberPriority(SoberBooking.SoberBookingStatus status) {
        if (status == SoberBooking.SoberBookingStatus.IN_PROGRESS) {
            return 0;
        }
        if (status == SoberBooking.SoberBookingStatus.ARRIVED) {
            return 1;
        }
        return 2;
    }

    private int rentalPriority(VehicleRental.RentalStatus status) {
        if (status == VehicleRental.RentalStatus.ACTIVE) {
            return 0;
        }
        return 1;
    }

    private DriverActiveTripResponse toSoberResponse(SoberBooking booking) {
        DriverActiveTripResponse response = new DriverActiveTripResponse();
        response.setHasActiveTrip(true);
        response.setTripType("SOBER");
        response.setTripId(booking.getId());
        response.setStatus(booking.getStatus().name());
        response.setPickupAddress(booking.getPickupAddress());
        response.setShouldTrackLocation(true);
        response.setCanStartTrip(booking.getStatus() == SoberBooking.SoberBookingStatus.ARRIVED);
        response.setCanCompleteTrip(booking.getStatus() == SoberBooking.SoberBookingStatus.IN_PROGRESS);
        return response;
    }

    private DriverActiveTripResponse toRentalResponse(VehicleRental rental) {
        DriverActiveTripResponse response = new DriverActiveTripResponse();
        response.setHasActiveTrip(true);
        response.setTripType("RENTAL");
        response.setTripId(rental.getId());
        response.setStatus(rental.getStatus().name());
        response.setPickupAddress(rental.getPickupAddress());
        response.setShouldTrackLocation(true);
        response.setCanStartTrip(rental.getStatus() == VehicleRental.RentalStatus.CONFIRMED);
        response.setCanCompleteTrip(rental.getStatus() == VehicleRental.RentalStatus.ACTIVE);
        return response;
    }
}
