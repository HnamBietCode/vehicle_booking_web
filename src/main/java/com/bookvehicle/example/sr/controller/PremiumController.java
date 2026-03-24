package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.Customer;
import com.bookvehicle.example.sr.model.PremiumTier;
import com.bookvehicle.example.sr.model.Role;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.service.PremiumService;
import com.bookvehicle.example.sr.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bookvehicle.example.sr.model.Wallet;
import java.util.Optional;

@Controller
@RequestMapping("/premium")
public class PremiumController {

    private final PremiumService premiumService;
    private final WalletService walletService;

    public PremiumController(PremiumService premiumService, WalletService walletService) {
        this.premiumService = premiumService;
        this.walletService = walletService;
    }

    @GetMapping
    public String index(Model model, HttpSession session) {
        User user = SecurityUtil.getLoggedUser(session);
        model.addAttribute("user", user);
        model.addAttribute("premiumTiers", premiumService.getAvailableTiers());

        if (user.getRole() == Role.CUSTOMER) {
            premiumService.findByUserId(user.getId())
                    .ifPresent(customer -> model.addAttribute("customer", customer));
        }
        return "premium/index";
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam("tier") String rawTier,
                           HttpSession session,
                           Model model,
                           RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        if (user.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Chi tai khoan Khach moi co the nang cap Premium.");
            return "redirect:/premium";
        }

        PremiumTier tier = premiumService.parseTier(rawTier);
        if (tier == null) {
            ra.addFlashAttribute("error", "Khong tim thay goi premium ban da chon.");
            return "redirect:/premium";
        }

        Optional<Customer> customer = premiumService.findByUserId(user.getId());
        if (customer.isEmpty()) {
            ra.addFlashAttribute("error", "Khong tim thay ho so khach hang.");
            return "redirect:/premium";
        }

        Wallet wallet = walletService.getOrCreateWallet(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("customer", customer.get());
        model.addAttribute("selectedTier", tier);
        model.addAttribute("wallet", wallet);
        model.addAttribute("canAfford", wallet.getBalance().compareTo(tier.getPrice()) >= 0);
        return "premium/checkout";
    }

    @PostMapping("/purchase")
    public String purchase(@RequestParam("tier") String rawTier,
                           HttpSession session,
                           RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        if (user.getRole() != Role.CUSTOMER) {
            ra.addFlashAttribute("error", "Chi tai khoan Khach moi co the nang cap Premium.");
            return "redirect:/premium";
        }

        PremiumTier tier = premiumService.parseTier(rawTier);
        if (tier == null) {
            ra.addFlashAttribute("error", "Khong tim thay goi premium ban da chon.");
            return "redirect:/premium";
        }

        PremiumService.PurchaseResult result = premiumService.purchase(user.getId(), tier);
        if (result.ok()) {
            ra.addFlashAttribute("success", result.message());
            return "redirect:/premium";
        }

        ra.addFlashAttribute("error", result.message());
        return "redirect:/premium/checkout?tier=" + tier.name();
    }

    @PostMapping("/cancel")
    public String cancel(HttpSession session, RedirectAttributes ra) {
        User user = SecurityUtil.getLoggedUser(session);
        String error = premiumService.cancel(user.getId());
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Tai khoan da tro ve goi Standard.");
        }
        return "redirect:/premium";
    }
}
