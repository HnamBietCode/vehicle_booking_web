package com.bookvehicle.example.sr.controller.driver;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.RentalCompleteForm;
import com.bookvehicle.example.sr.dto.ReportDashboardDTO;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.VehicleRental;
import com.bookvehicle.example.sr.service.DriverReportService;
import com.bookvehicle.example.sr.service.VehicleRentalService;
import com.bookvehicle.example.sr.repository.DriverRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/driver/rentals")
public class DriverRentalController {

    private final VehicleRentalService vehicleRentalService;
    private final DriverReportService driverReportService;
    private final DriverRepository driverRepository;

    public DriverRentalController(VehicleRentalService vehicleRentalService, 
                                  DriverReportService driverReportService,
                                  DriverRepository driverRepository) {
        this.vehicleRentalService = vehicleRentalService;
        this.driverReportService = driverReportService;
        this.driverRepository = driverRepository;
    }

    @GetMapping
    public String index(HttpSession session, Model model, RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.DRIVER) {
            ra.addFlashAttribute("error", "Ban khong co quyen truy cap.");
            return "redirect:/";
        }

        if (driverRepository.findByUserId(loggedUser.getId()).isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy hồ sơ tài xế của bạn. Vui lòng liên hệ Admin.");
            return "redirect:/profile";
        }

        List<VehicleRental> rentals = vehicleRentalService.findByDriverUserId(loggedUser.getId());
        List<VehicleRental> pendingVehicleOnly = vehicleRentalService.findPendingVehicleOnly();
        
        // Add daily stats for the dashboard header
        ReportDashboardDTO todayStats = driverReportService.getDashboardData(loggedUser.getId(), "daily");
        
        model.addAttribute("rentals", rentals);
        model.addAttribute("pendingVehicleOnly", pendingVehicleOnly);
        model.addAttribute("todayStats", todayStats);
        model.addAttribute("completeForm", new RentalCompleteForm());
        model.addAttribute("loggedUser", loggedUser);
        return "driver/rentals";
    }

    @PostMapping("/{id}/complete")
    public String complete(
            @PathVariable("id") Long rentalId,
            @ModelAttribute("completeForm") RentalCompleteForm form,
            HttpSession session,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.DRIVER) {
            ra.addFlashAttribute("error", "Ban khong co quyen thuc hien.");
            return "redirect:/";
        }

        VehicleRentalService.ServiceResult result = vehicleRentalService.completeTrip(
                rentalId,
                loggedUser.getId(),
                form.getExtraFee(),
                form.getNotes()
        );
        if (result.ok()) {
            ra.addFlashAttribute("success", result.message());
        } else {
            ra.addFlashAttribute("error", result.message());
        }
        return "redirect:/driver/rentals";
    }

    @PostMapping("/{id}/accept")
    public String accept(
            @PathVariable("id") Long rentalId,
            HttpSession session,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.DRIVER) {
            ra.addFlashAttribute("error", "Ban khong co quyen thuc hien.");
            return "redirect:/";
        }

        VehicleRentalService.ServiceResult result = vehicleRentalService.acceptRental(rentalId, loggedUser.getId());
        if (result.ok()) {
            ra.addFlashAttribute("success", result.message());
        } else {
            ra.addFlashAttribute("error", result.message());
        }
        return "redirect:/driver/rentals";
    }

    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable("id") Long rentalId,
            @RequestParam(name = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.DRIVER) {
            ra.addFlashAttribute("error", "Ban khong co quyen thuc hien.");
            return "redirect:/";
        }

        VehicleRentalService.ServiceResult result = vehicleRentalService.rejectRental(rentalId, loggedUser.getId(), reason);
        if (result.ok()) {
            ra.addFlashAttribute("success", result.message());
        } else {
            ra.addFlashAttribute("error", result.message());
        }
        return "redirect:/driver/rentals";
    }

    @PostMapping("/{id}/start")
    public String start(
            @PathVariable("id") Long rentalId,
            HttpSession session,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }
        if (loggedUser.getRole() != Role.DRIVER) {
            ra.addFlashAttribute("error", "Ban khong co quyen thuc hien.");
            return "redirect:/";
        }

        VehicleRentalService.ServiceResult result = vehicleRentalService.startTrip(rentalId, loggedUser.getId());
        if (result.ok()) {
            ra.addFlashAttribute("success", result.message());
        } else {
            ra.addFlashAttribute("error", result.message());
        }
        return "redirect:/driver/rentals";
    }
}
