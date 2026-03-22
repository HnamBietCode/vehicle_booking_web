package com.bookvehicle.example.sr.controller.admin;

import com.bookvehicle.example.sr.config.SecurityUtil;
import com.bookvehicle.example.sr.model.WithdrawalRequest;
import com.bookvehicle.example.sr.repository.UserRepository;
import com.bookvehicle.example.sr.service.WithdrawalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/withdrawals")
public class WithdrawalManagementController {

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String index(@RequestParam(name = "status", required = false) String status,
                        Model model, HttpSession session) {
        List<WithdrawalRequest> withdrawals;
        if ("PENDING".equalsIgnoreCase(status)) {
            withdrawals = withdrawalService.findPending();
        } else {
            withdrawals = withdrawalService.findAll();
        }
        model.addAttribute("withdrawals", withdrawals);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("loggedUser", SecurityUtil.getLoggedUser(session));
        return "admin/withdrawals/index";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(name = "adminNote", required = false) String adminNote,
                          RedirectAttributes ra) {
        String error = withdrawalService.approve(id, adminNote);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Đã duyệt yêu cầu rút tiền #" + id);
        }
        return "redirect:/admin/withdrawals";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(name = "adminNote", required = false) String adminNote,
                         RedirectAttributes ra) {
        String error = withdrawalService.reject(id, adminNote);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Đã từ chối yêu cầu rút tiền #" + id + ". Tiền đã được hoàn lại ví.");
        }
        return "redirect:/admin/withdrawals";
    }
}
