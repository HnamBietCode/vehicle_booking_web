package com.bookvehicle.example.sr.controller.driver;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.SoberBooking;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import com.bookvehicle.example.sr.service.SoberBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver/sober-bookings")
public class DriverSoberBookingRestController {

    private final SoberBookingService soberBookingService;
    private final SoberBookingRepository soberBookingRepository;
    private final DriverRepository driverRepository;

    public DriverSoberBookingRestController(SoberBookingService soberBookingService,
                                            SoberBookingRepository soberBookingRepository,
                                            DriverRepository driverRepository) {
        this.soberBookingService = soberBookingService;
        this.soberBookingRepository = soberBookingRepository;
        this.driverRepository = driverRepository;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SoberBooking>> pending(HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(soberBookingRepository.findPendingBookings());
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<SoberBooking>> assigned(HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        return driverRepository.findByUserId(loggedUser.getId())
                .map(driver -> ResponseEntity.ok(soberBookingRepository.findByDriverId(driver.getId())))
                .orElse(ResponseEntity.ok(List.of()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<String> accept(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        try {
            soberBookingService.acceptBooking(id, loggedUser.getId());
            return ResponseEntity.ok("Da nhan don.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/arrive")
    public ResponseEntity<String> arrive(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        try {
            soberBookingService.arriveAtPickup(id, loggedUser.getId());
            return ResponseEntity.ok("Da den diem don.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<String> start(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        try {
            soberBookingService.startTrip(id, loggedUser.getId());
            return ResponseEntity.ok("Da bat dau chuyen.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<String> complete(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        try {
            soberBookingService.completeTrip(id, loggedUser.getId());
            return ResponseEntity.ok("Da hoan thanh chuyen.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
