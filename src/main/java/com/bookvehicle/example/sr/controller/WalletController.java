package com.bookvehicle.example.sr.controller;

import com.bookvehicle.example.sr.model.Transaction;
import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.Wallet;
import com.bookvehicle.example.sr.service.MomoService;
import com.bookvehicle.example.sr.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private MomoService momoService;

    @GetMapping("/wallet")
    public String showWallet(HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        Wallet wallet = walletService.getOrCreateWallet(loggedUser.getId());
        List<Transaction> transactions = walletService.getTransactionHistory(wallet.getId());

        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions);

        return "wallets/index";
    }

    @PostMapping("/wallet/deposit/momo")
    public String depositMomo(@RequestParam("amount") BigDecimal amount, HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        if (amount == null || amount.compareTo(new BigDecimal("10000")) < 0) {
            model.addAttribute("error", "Số tiền nạp tối thiểu là 10,000" + "₫");
            return "forward:/wallet"; // re-render wallet page with error
        }

        String payUrl = momoService.createDepositRequest(loggedUser.getId(), amount);

        if (payUrl != null && !payUrl.isEmpty()) {
            return "redirect:" + payUrl;
        } else {
            model.addAttribute("error", "Lỗi kết nối tới cổng thanh toán MoMo. Vui lòng thử lại sau.");
            return "forward:/wallet";
        }
    }

    @GetMapping("/wallet/deposit/momo/return")
    public String momoReturn(
            @RequestParam("orderId") String orderIdMoMo,
            @RequestParam("amount") String amountStr,
            @RequestParam("resultCode") Integer resultCode,
            HttpSession session,
            Model model) {

        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            // Recover user from orderIdMoMo string (DEP_{userId}_{timestamp})
            try {
                String[] parts = orderIdMoMo.split("_");
                Long userId = Long.parseLong(parts[1]);
                // Logged out mid-transaction, but still process payment
                processPayment(userId, amountStr, resultCode, orderIdMoMo);
                return "redirect:/login?msg=Nap tien thanh cong. Vui long dang nhap lai.";
            } catch (Exception e) {
                return "redirect:/login";
            }
        }

        boolean success = processPayment(loggedUser.getId(), amountStr, resultCode, orderIdMoMo);

        if (success) {
            return "redirect:/wallet?success=momo_deposit_success";
        } else {
            return "redirect:/wallet?error=momo_payment_failed";
        }
    }

    private boolean processPayment(Long userId, String amountStr, Integer resultCode, String orderIdMoMo) {
        if (resultCode == 0) {
            BigDecimal amount = new BigDecimal(amountStr);
            String description = "Nạp tiền vào ví qua MoMo. Mã GD: " + orderIdMoMo;
            // ReferenceId is not strictly needed here since it's a deposit
            walletService.deposit(userId, amount, description, null);
            return true;
        }
        return false;
    }
}
