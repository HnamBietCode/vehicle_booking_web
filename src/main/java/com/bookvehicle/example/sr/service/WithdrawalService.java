package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.model.*;
import com.bookvehicle.example.sr.repository.WithdrawalRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WithdrawalService {

    @Autowired
    private WithdrawalRequestRepository withdrawalRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private NotificationService notificationService;

    /**
     * User/Driver tạo lệnh rút tiền.
     * Trừ tiền trong ví ngay lập tức (hold) và tạo request PENDING.
     */
    @Transactional
    public String createRequest(Long userId, BigDecimal amount,
                                 String bankName, String accountNumber,
                                 String accountHolder, String note) {
        if (amount == null || amount.compareTo(new BigDecimal("10000")) < 0) {
            return "Số tiền rút tối thiểu là 10,000₫.";
        }

        // Trừ tiền trong ví (hold)
        Wallet wallet = walletService.getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            return "Số dư ví không đủ để rút.";
        }

        // Tạo withdrawal request
        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setUserId(userId);
        wr.setAmount(amount);
        wr.setBankName(bankName != null ? bankName.trim() : null);
        wr.setAccountNumber(accountNumber != null ? accountNumber.trim() : null);
        wr.setAccountHolder(accountHolder != null ? accountHolder.trim() : null);
        wr.setNote(note != null ? note.trim() : null);
        wr.setStatus(WithdrawalStatus.PENDING);
        withdrawalRepository.save(wr);

        // Trừ tiền ví (hold cho lệnh rút)
        walletService.debit(
                userId, amount,
                TransactionType.WITHDRAW,
                ReferenceType.WITHDRAWAL,
                wr.getId(),
                "Yêu cầu rút tiền #" + wr.getId() + " - " + bankName + " " + accountNumber
        );

        // Notification for user
        try {
            notificationService.createNotification(
                    userId, "Yêu cầu rút tiền",
                    "Yêu cầu rút " + amount + "₫ đã được gửi. Chờ admin duyệt.",
                    NotificationType.WITHDRAW_REQUEST,
                    NotificationRefType.WALLET, wr.getId());
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Admin duyệt lệnh rút → đổi trạng thái thành APPROVED.
     */
    @Transactional
    public String approve(Long requestId, String adminNote) {
        Optional<WithdrawalRequest> opt = withdrawalRepository.findById(requestId);
        if (opt.isEmpty()) return "Không tìm thấy yêu cầu rút tiền.";

        WithdrawalRequest wr = opt.get();
        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            return "Yêu cầu này đã được xử lý.";
        }

        wr.setStatus(WithdrawalStatus.APPROVED);
        wr.setAdminNote(adminNote);
        wr.setProcessedAt(LocalDateTime.now());
        withdrawalRepository.save(wr);

        // Thông báo cho user
        try {
            notificationService.createNotification(
                    wr.getUserId(), "Rút tiền được duyệt",
                    "Yêu cầu rút " + wr.getAmount() + "₫ đã được duyệt." + (adminNote != null ? " Note: " + adminNote : ""),
                    NotificationType.WITHDRAW_APPROVED,
                    NotificationRefType.WALLET, wr.getId());
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Admin từ chối lệnh rút → hoàn tiền lại ví.
     */
    @Transactional
    public String reject(Long requestId, String adminNote) {
        Optional<WithdrawalRequest> opt = withdrawalRepository.findById(requestId);
        if (opt.isEmpty()) return "Không tìm thấy yêu cầu rút tiền.";

        WithdrawalRequest wr = opt.get();
        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            return "Yêu cầu này đã được xử lý.";
        }

        // Hoàn tiền lại ví
        walletService.credit(
                wr.getUserId(), wr.getAmount(),
                TransactionType.REFUND,
                ReferenceType.WITHDRAWAL,
                wr.getId(),
                "Hoàn tiền yêu cầu rút #" + wr.getId() + " bị từ chối"
        );

        wr.setStatus(WithdrawalStatus.REJECTED);
        wr.setAdminNote(adminNote);
        wr.setProcessedAt(LocalDateTime.now());
        withdrawalRepository.save(wr);

        // Thông báo từ chối
        try {
            notificationService.createNotification(
                    wr.getUserId(), "Rút tiền bị từ chối",
                    "Yêu cầu rút " + wr.getAmount() + "₫ đã bị từ chối và hoàn tiền về ví." + (adminNote != null ? " Lý do: " + adminNote : ""),
                    NotificationType.WITHDRAW_REJECTED,
                    NotificationRefType.WALLET, wr.getId());
        } catch (Exception ignored) {}
        return null;
    }

    @Transactional(readOnly = true)
    public List<WithdrawalRequest> findByUser(Long userId) {
        return withdrawalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<WithdrawalRequest> findPending() {
        return withdrawalRepository.findByStatusOrderByCreatedAtAsc(WithdrawalStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<WithdrawalRequest> findAll() {
        return withdrawalRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<WithdrawalRequest> findById(Long id) {
        return withdrawalRepository.findById(id);
    }
}
