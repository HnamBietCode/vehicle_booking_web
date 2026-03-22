package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.Customer;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.service.PremiumService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/premium")
public class PremiumController {

    private final PremiumService premiumService;

    public PremiumController(PremiumService premiumService) {
        this.premiumService = premiumService;
    }

    // ── Index – Trang giới thiệu Premium ─────────────────────────

    @GetMapping
    public String index(Model model, HttpSession session) {
        User user = SecurityUtil.getLoggedUser(session);
        model.addAttribute("user", user);

        if (user.getRole() == Role.CUSTOMER) {
            premiumService.findByUserId(user.getId())
                          .ifPresent(c -> model.addAttribute("customer", c));
        }
        return "premium/index";
    }

    // ── Upgrade ───────────────────────────────────────────────────

    @PostMapping("/upgrade")
    public String upgrade(@RequestParam(defaultValue = "1") int months,
                           HttpSession session,
                           RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        if (user.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Chỉ tài khoản Khách mới có thể nâng cấp Premium.");
            return "redirect:/premium";
        }
        String error = premiumService.upgrade(user.getId(), months);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success",
                    "Nâng cấp Premium thành công! Thời hạn: " + months + " tháng.");
        }
        return "redirect:/premium";
    }

    // ── Cancel ────────────────────────────────────────────────────

    @PostMapping("/cancel")
    public String cancel(HttpSession session, RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        String error = premiumService.cancel(user.getId());
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Đã huỷ Premium. Tài khoản trở về Standard.");
        }
        return "redirect:/premium";
    }
}
