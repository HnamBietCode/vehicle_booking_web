package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.dto.ProfileEditForm;
import com.bookvehicle.example.sr.dto.RegisterForm;
import com.bookvehicle.example.sr.dto.VehicleForm;
import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.service.UserService;
import com.bookvehicle.example.sr.service.VehicleService;
import com.bookvehicle.example.sr.service.RatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final RatingService ratingService;

    public UserManagementController(UserService userService, VehicleService vehicleService,
            RatingService ratingService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.ratingService = ratingService;
    }

    // ── Index – Danh sách user ────────────────────────────────────

    @GetMapping
    public String index(Model model, HttpSession session) {
        List<User> users = userService.findAll();

        java.util.Map<Long, String> driverStatuses = new java.util.HashMap<>();
        for (User u : users) {
            if (u.getRole().name().equals("DRIVER")) {
                userService.findDriverByUserId(u.getId())
                        .ifPresent(d -> driverStatuses.put(u.getId(), d.getVerificationStatus().name()));
            }
        }

        model.addAttribute("driverStatuses", driverStatuses);
        model.addAttribute("users", users);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/users/index";
    }

    // ── Detail – Chi tiết user ────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty())
            return "redirect:/admin/users?error=notfound";
        User target = opt.get();
        model.addAttribute("target", target);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));

        // Load customer/driver profile
        if (target.getRole().name().equals("CUSTOMER")) {
            userService.findCustomerByUserId(id).ifPresent(c -> model.addAttribute("customer", c));
        } else if (target.getRole().name().equals("DRIVER")) {
            userService.findDriverByUserId(id).ifPresent(d -> {
                model.addAttribute("driver", d);

                int totalRatings = ratingService.findDriverRatings(d.getId()).size();
                Double avgRating = ratingService.getAvgRating(RatingTargetType.DRIVER, d.getId());
                model.addAttribute("totalRatings", totalRatings);
                model.addAttribute("avgRating", String.format("%.2f", avgRating));
            });
        }
        return "admin/users/detail";
    }

    // ── Add – Thêm user mới ───────────────────────────────────────

    @GetMapping("/add")
    public String showAddForm(Model model, HttpSession session) {
        model.addAttribute("form", new RegisterForm());
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/users/add";
    }

    @PostMapping("/add")
    public String handleAdd(@ModelAttribute("form") RegisterForm form,
            Model model,
            HttpSession session,
            RedirectAttributes ra) {
        String error = userService.adminCreateUser(form);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
            return "admin/users/add";
        }
        ra.addFlashAttribute("success", "Thêm tài khoản mới thành công.");
        return "redirect:/admin/users";
    }

    // ── Edit – Sửa user ───────────────────────────────────────────

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty())
            return "redirect:/admin/users?error=notfound";
        User target = opt.get();

        ProfileEditForm form = new ProfileEditForm();
        form.setPhone(target.getPhone());
        form.setAvatarUrl(target.getAvatarUrl());

        if (target.getRole().name().equals("CUSTOMER")) {
            userService.findCustomerByUserId(id).ifPresent(c -> {
                form.setFullName(c.getFullName());
                form.setAddress(c.getAddress());
            });
        } else if (target.getRole().name().equals("DRIVER")) {
            userService.findDriverByUserId(id).ifPresent(d -> form.setFullName(d.getFullName()));
        } else {
            form.setFullName(target.getEmail());
        }

        model.addAttribute("form", form);
        model.addAttribute("target", target);
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/users/edit";
    }

    @PostMapping("/{id}/edit")
    public String handleEdit(@PathVariable Long id,
            @ModelAttribute("form") ProfileEditForm form,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            Model model,
            HttpSession session,
            RedirectAttributes ra) {
        String error = userService.adminUpdateUser(id, form, role, isActive);
        if (error != null) {
            model.addAttribute("error", error);
            model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
            userService.findById(id).ifPresent(u -> model.addAttribute("target", u));
            return "admin/users/edit";
        }
        ra.addFlashAttribute("success", "Cập nhật tài khoản thành công.");
        return "redirect:/admin/users/" + id;
    }

    // ── Delete – Xoá user ─────────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes ra) {
        // Ngăn admin tự xoá mình
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser.getId().equals(id)) {
            ra.addFlashAttribute("error", "Không thể xoá tài khoản đang đăng nhập.");
            return "redirect:/admin/users";
        }
        userService.delete(id);
        ra.addFlashAttribute("success", "Đã xoá tài khoản.");
        return "redirect:/admin/users";
    }

    // ── Toggle Active – Khoá / Mở khoá ───────────────────────────

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id,
            @RequestParam(name = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        if (loggedUser.getId().equals(id)) {
            ra.addFlashAttribute("error", "Không thể khoá tài khoản đang đăng nhập.");
            return "redirect:/admin/users";
        }
        userService.toggleActive(id, reason);
        ra.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản.");
        return "redirect:/admin/users/" + id;
    }

    // ── Verify Driver – Duyệt hồ sơ ──────────────────────────────

    @PostMapping("/{id}/verify-driver")
    public String verifyDriver(@PathVariable Long id,
            @RequestParam(name = "status") String status,
            @RequestParam(name = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes ra) {
        User loggedUser = SecurityUtil.getLoggedUser(session);
        String error = userService.verifyDriver(id, status, loggedUser.getId(), reason);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái duyệt hồ sơ tài xế.");
        }
        return "redirect:/admin/users/" + id;
    }
}
