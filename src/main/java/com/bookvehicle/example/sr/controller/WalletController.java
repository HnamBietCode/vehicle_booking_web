package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.model.Transaction;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.Wallet;
import com.bookvehicle.example.sr.model.WithdrawalRequest;
import com.bookvehicle.example.sr.service.MomoService;
import com.bookvehicle.example.sr.service.WalletService;
import com.bookvehicle.example.sr.service.WithdrawalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private MomoService momoService;

    @Autowired
    private WithdrawalService withdrawalService;

    @GetMapping("/wallet")
    public String showWallet(HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }

        Wallet wallet = walletService.getOrCreateWallet(loggedUser.getId());
        List<Transaction> transactions = walletService.getTransactionHistory(wallet.getId());
        List<WithdrawalRequest> withdrawals = withdrawalService.findByUser(loggedUser.getId());
        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions);
        model.addAttribute("withdrawals", withdrawals);
        model.addAttribute("loggedUser", loggedUser);
        return "wallets/index";
    }

    @PostMapping("/wallet/deposit/momo")
    public String depositMomo(@RequestParam("amount") BigDecimal amount,
                              HttpSession session,
                              RedirectAttributes ra) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            return "redirect:/auth/login";
        }

        if (amount == null || amount.compareTo(new BigDecimal("10000")) < 0) {
            ra.addFlashAttribute("error", "So tien nap toi thieu la 10,000 VND.");
            return "redirect:/wallet";
        }

        String payUrl = momoService.createDepositRequest(loggedUser.getId(), amount);
        if (payUrl != null && !payUrl.isEmpty()) {
            return "redirect:" + payUrl;
        }

        ra.addFlashAttribute("error", "Loi ket noi cong thanh toan MoMo. Vui long thu lai.");
        return "redirect:/wallet";
    }

    @GetMapping("/wallet/deposit/momo/return")
    public String momoReturn(
            @RequestParam("orderId") String orderIdMoMo,
            @RequestParam("amount") String amountStr,
            @RequestParam("resultCode") Integer resultCode,
            HttpSession session) {

        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            try {
                String[] parts = orderIdMoMo.split("_");
                Long userId = Long.parseLong(parts[1]);
                processPayment(userId, amountStr, resultCode, orderIdMoMo);
                return "redirect:/auth/login?msg=Nap tien thanh cong. Vui long dang nhap lai.";
            } catch (Exception e) {
                return "redirect:/auth/login";
            }
        }

        boolean success = processPayment(loggedUser.getId(), amountStr, resultCode, orderIdMoMo);
        if (success) {
            return "redirect:/wallet?success=momo_deposit_success";
        }
        return "redirect:/wallet?error=momo_payment_failed";
    }

    // ── Rút tiền ────────────────────────────────────────────────────

    @PostMapping("/wallet/withdraw")
    public String withdraw(@RequestParam("amount") BigDecimal amount,
                            @RequestParam("bankName") String bankName,
                            @RequestParam("accountNumber") String accountNumber,
                            @RequestParam("accountHolder") String accountHolder,
                            @RequestParam(name = "note", required = false) String note,
                            HttpSession session,
                            RedirectAttributes ra) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/auth/login";

        String error = withdrawalService.createRequest(
                loggedUser.getId(), amount, bankName, accountNumber, accountHolder, note);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        } else {
            ra.addFlashAttribute("success", "Yêu cầu rút tiền đã được tạo. Vui lòng chờ Admin xử lý.");
        }
        return "redirect:/wallet";
    }

    private boolean processPayment(Long userId, String amountStr, Integer resultCode, String orderIdMoMo) {
        if (resultCode == 0) {
            BigDecimal amount = new BigDecimal(amountStr);
            String description = "Nap tien vao vi qua MoMo. Ma GD: " + orderIdMoMo;
            walletService.deposit(userId, amount, description, null);
            return true;
        }
        return false;
    }
}

