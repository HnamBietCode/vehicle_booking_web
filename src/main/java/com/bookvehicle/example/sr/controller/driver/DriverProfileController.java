package com.bookvehicle.example.sr.controller.driver;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.ProfileEditForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.service.RatingService;
import com.bookvehicle.example.sr.service.UserService;
import com.bookvehicle.example.sr.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/driver/profile")
public class DriverProfileController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final RatingService ratingService;

    public DriverProfileController(UserService userService, VehicleService vehicleService,
                                    RatingService ratingService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.ratingService = ratingService;
    }

    @GetMapping
    public String detail(Model model, HttpSession session, RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        if (user == null) return "redirect:/auth/login";
        if (user.getRole() != Role.DRIVER) {
            return "redirect:/profile";
        }

        model.addAttribute("user", user);

        ProfileEditForm form = new ProfileEditForm();
        form.setPhone(user.getPhone());
        form.setAvatarUrl(user.getAvatarUrl());

        userService.findDriverByUserId(user.getId()).ifPresent(d -> {
            model.addAttribute("driver", d);
            form.setFullName(d.getFullName());
            form.setVehicleTypes(d.getVehicleTypes());

            // Rating Stats
            int totalRatings = ratingService.findDriverRatings(d.getId()).size();
            Double avgRating = ratingService.getAvgRating(RatingTargetType.DRIVER, d.getId());
            model.addAttribute("totalRatings", totalRatings);
            model.addAttribute("avgRating", String.format("%.2f", avgRating));

            // Assigned Vehicle
            vehicleService.findByAssignedDriver(d.getId())
                          .ifPresent(v -> model.addAttribute("assignedVehicle", v));
        });

        if (form.getFullName() == null) {
            form.setFullName(user.getEmail());
        }
        model.addAttribute("form", form);

        return "driver/profile";
    }

    @PostMapping("/edit")
    public String handleEdit(@ModelAttribute("form") ProfileEditForm form,
                              HttpSession session,
                              RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        if (user == null) return "redirect:/auth/login";
        if (user.getRole() != Role.DRIVER) return "redirect:/profile";

        String error = userService.updateProfile(user.getId(), form);
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "redirect:/driver/profile";
        }
        // Refresh user in session
        userService.findById(user.getId())
                   .ifPresent(u -> SecurityUtil.setLoggedUser(session, u));
        ra.addFlashAttribute("success", "Hồ sơ đã được cập nhật thành công.");
        return "redirect:/driver/profile";
    }
}
