package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.repository.DeviceTokenRepository;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import com.bookvehicle.example.sr.repository.VehicleRentalRepository;
import com.bookvehicle.example.sr.service.RealtimeStatsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/realtime")
public class AdminRealtimeRestController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final DriverRepository driverRepository;
    private final SoberBookingRepository soberBookingRepository;
    private final VehicleRentalRepository vehicleRentalRepository;
    private final RealtimeStatsService realtimeStatsService;

    public AdminRealtimeRestController(DeviceTokenRepository deviceTokenRepository,
                                       DriverRepository driverRepository,
                                       SoberBookingRepository soberBookingRepository,
                                       VehicleRentalRepository vehicleRentalRepository,
                                       RealtimeStatsService realtimeStatsService) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.driverRepository = driverRepository;
        this.soberBookingRepository = soberBookingRepository;
        this.vehicleRentalRepository = vehicleRentalRepository;
        this.realtimeStatsService = realtimeStatsService;
    }

    public record RealtimeStats(
            long deviceTokenCount,
            long driverOnlineCount,
            long pendingSoberCount,
            long pendingVehicleOnlyCount,
            long activeLocationCount
    ) {}

    @GetMapping("/stats")
    public ResponseEntity<RealtimeStats> stats(HttpSession session, HttpServletRequest request) {
        User loggedUser = SecurityUtil.getLoggedUser(session, request);
        if (loggedUser == null || loggedUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(401).build();
        }
        RealtimeStats stats = new RealtimeStats(
                deviceTokenRepository.count(),
                driverRepository.countOnlineApproved(),
                soberBookingRepository.countPending(),
                vehicleRentalRepository.countPendingVehicleOnly(),
                realtimeStatsService.countActiveDriverLocations()
        );
        return ResponseEntity.ok(stats);
    }
}
