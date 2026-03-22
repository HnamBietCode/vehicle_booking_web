package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.repository.DeviceTokenRepository;
import com.bookvehicle.example.sr.repository.DriverRepository;
import com.bookvehicle.example.sr.repository.SoberBookingRepository;
import com.bookvehicle.example.sr.repository.VehicleRentalRepository;
import com.bookvehicle.example.sr.service.RealtimeStatsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/realtime")
public class AdminRealtimeController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final DriverRepository driverRepository;
    private final SoberBookingRepository soberBookingRepository;
    private final VehicleRentalRepository vehicleRentalRepository;
    private final RealtimeStatsService realtimeStatsService;

    public AdminRealtimeController(DeviceTokenRepository deviceTokenRepository,
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

    @GetMapping
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.ADMIN) {
            ra.addFlashAttribute("error", "Ban khong co quyen truy cap.");
            return "redirect:/";
        }
        model.addAttribute("deviceTokenCount", deviceTokenRepository.count());
        model.addAttribute("driverOnlineCount", driverRepository.countOnlineApproved());
        model.addAttribute("pendingSoberCount", soberBookingRepository.countPending());
        model.addAttribute("pendingVehicleOnlyCount", vehicleRentalRepository.countPendingVehicleOnly());
        model.addAttribute("activeLocationCount", realtimeStatsService.countActiveDriverLocations());
        model.addAttribute("loggedUser", loggedUser);
        return "admin/realtime";
    }
}
