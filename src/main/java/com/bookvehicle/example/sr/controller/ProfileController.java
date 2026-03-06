package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.ChangePasswordForm;
import com.bookvehicle.example.sr.dto.ProfileEditForm;
import com.bookvehicle.example.sr.model.Customer;
import com.bookvehicle.example.sr.model.Driver;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.Vehicle;
import com.bookvehicle.example.sr.model.RatingTargetType;
import com.bookvehicle.example.sr.service.UserService;
import com.bookvehicle.example.sr.service.VehicleService;
import com.bookvehicle.example.sr.service.RatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final RatingService ratingService;

    public ProfileController(UserService userService, VehicleService vehicleService, RatingService ratingService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.ratingService = ratingService;
    }

    // ── Detail ────────────────────────────────────────────────────

    @GetMapping
    public String detail(Model model, HttpSession session) {
        User user = SecurityUtil.getLoggedUser(session);
        model.addAttribute("user", user);
        if (user.getRole() == Role.CUSTOMER) {
            userService.findCustomerByUserId(user.getId())
                       .ifPresent(c -> model.addAttribute("customer", c));
        } else if (user.getRole() == Role.DRIVER) {
            userService.findDriverByUserId(user.getId())
                       .ifPresent(d -> {
                           model.addAttribute("driver", d);
                           
                           // Add Rating Stats
                           int totalRatings = ratingService.findDriverRatings(d.getId()).size();
                           Double avgRating = ratingService.getAvgRating(RatingTargetType.DRIVER, d.getId());
                           model.addAttribute("totalRatings", totalRatings);
                           model.addAttribute("avgRating", String.format("%.2f", avgRating));
                           
                           // Add Assigned Vehicle
                           vehicleService.findByAssignedDriver(d.getId())
                                         .ifPresent(v -> model.addAttribute("assignedVehicle", v));
                       });
        }
        return "profile/detail";
    }

    // ── Edit ──────────────────────────────────────────────────────

    @GetMapping("/edit")
    public String showEditForm(Model model, HttpSession session) {
        User user = SecurityUtil.getLoggedUser(session);
        ProfileEditForm form = new ProfileEditForm();
        form.setPhone(user.getPhone());
        form.setAvatarUrl(user.getAvatarUrl());

        if (user.getRole() == Role.CUSTOMER) {
            userService.findCustomerByUserId(user.getId()).ifPresent(c -> {
                form.setFullName(c.getFullName());
                form.setAddress(c.getAddress());
            });
        } else if (user.getRole() == Role.DRIVER) {
            userService.findDriverByUserId(user.getId()).ifPresent(d ->
                form.setFullName(d.getFullName()));
        } else {
            form.setFullName(user.getEmail()); // Admin không có fullName riêng
        }

        model.addAttribute("form", form);
        model.addAttribute("user", user);
        return "profile/edit";
    }

    @PostMapping("/edit")
    public String handleEdit(@ModelAttribute("form") ProfileEditForm form,
                              HttpSession session,
                              Model model,
                              RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        String error = userService.updateProfile(user.getId(), form);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("user", user);
            return "profile/edit";
        }
        // Refresh user in session
        userService.findById(user.getId())
                   .ifPresent(u -> SecurityUtil.setLoggedUser(session, u));
        ra.addFlashAttribute("success", "Hồ sơ đã được cập nhật thành công.");
        return "redirect:/profile";
    }

    // ── Update Location ───────────────────────────────────────────

    @PostMapping("/update-location")
    public String updateLocation(@RequestParam String location, HttpSession session, RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        if (user != null && user.getRole() == Role.DRIVER) {
            userService.findDriverByUserId(user.getId()).ifPresent(d -> {
                vehicleService.findByAssignedDriver(d.getId()).ifPresent(v -> {
                    vehicleService.updateLocation(v.getId(), location);
                    ra.addFlashAttribute("success", "Vị trí xe đã được cập nhật thành công!");
                });
            });
        }
        return "redirect:/profile";
    }

    // ── Change Password ───────────────────────────────────────────

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model, HttpSession session) {
        model.addAttribute("form", new ChangePasswordForm());
        model.addAttribute("user", SecurityUtil.getLoggedUser(session));
        return "profile/change-password";
    }

    @PostMapping("/change-password")
    public String handleChangePassword(@ModelAttribute("form") ChangePasswordForm form,
                                        HttpSession session,
                                        Model model,
                                        RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        String error = userService.changePassword(user.getId(), form);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("user", user);
            return "profile/change-password";
        }
        // Buộc đăng nhập lại vì mật khẩu đã đổi
        SecurityUtil.clearSession(session);
        ra.addFlashAttribute("success", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
        return "redirect:/auth/login";
    }
}
