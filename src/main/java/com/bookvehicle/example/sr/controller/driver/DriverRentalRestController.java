package com.bookvehicle.example.sr.controller.driver;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.VehicleRental;
import com.bookvehicle.example.sr.service.VehicleRentalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/driver/rentals")
public class DriverRentalRestController {

    private final VehicleRentalService vehicleRentalService;

    public DriverRentalRestController(VehicleRentalService vehicleRentalService) {
        this.vehicleRentalService = vehicleRentalService;
    }

    @GetMapping("/pending-vehicle-only")
    public ResponseEntity<List<VehicleRental>> pendingVehicleOnly(HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(vehicleRentalService.findPendingVehicleOnly());
    }

    @GetMapping("/pending-all")
    public ResponseEntity<List<VehicleRental>> pendingAll(HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(vehicleRentalService.findAllPending());
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<VehicleRental>> assigned(HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(vehicleRentalService.findByDriverUserId(loggedUser.getId()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<String> accept(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        VehicleRentalService.ServiceResult result = vehicleRentalService.acceptRental(id, loggedUser.getId());
        if (result.ok()) {
            return ResponseEntity.ok(result.message());
        }
        return ResponseEntity.badRequest().body(result.message());
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<String> start(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        VehicleRentalService.ServiceResult result = vehicleRentalService.startTrip(id, loggedUser.getId());
        if (result.ok()) {
            return ResponseEntity.ok(result.message());
        }
        return ResponseEntity.badRequest().body(result.message());
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<String> complete(@PathVariable Long id,
                                           @RequestParam(name = "extraFee", required = false) BigDecimal extraFee,
                                           @RequestParam(name = "notes", required = false) String notes,
                                           HttpSession session,
                                           HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.DRIVER) {
            return ResponseEntity.status(401).build();
        }
        VehicleRentalService.ServiceResult result = vehicleRentalService.completeTrip(id, loggedUser.getId(), extraFee, notes);
        if (result.ok()) {
            return ResponseEntity.ok(result.message());
        }
        return ResponseEntity.badRequest().body(result.message());
    }
}
