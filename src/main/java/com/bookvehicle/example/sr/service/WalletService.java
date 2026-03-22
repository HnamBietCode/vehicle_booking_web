package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.model.ReferenceType;
import com.bookvehicle.example.sr.model.Transaction;
import com.bookvehicle.example.sr.model.TransactionType;
import com.bookvehicle.example.sr.model.Wallet;
import com.bookvehicle.example.sr.repository.TransactionRepository;
import com.bookvehicle.example.sr.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        Optional<Wallet> optWallet = walletRepository.findByUserId(userId);
        if (optWallet.isPresent()) {
            return optWallet.get();
        }

        Wallet newWallet = new Wallet();
        newWallet.setUserId(userId);
        newWallet.setBalance(BigDecimal.ZERO);
        return walletRepository.save(newWallet);
    }

    @Transactional
    public void deposit(Long userId, BigDecimal amount, String description, Long referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("So tien nap phai lon hon 0");
        }

        Wallet wallet = getOrCreateWallet(userId);
        applyWalletMutation(
                wallet,
                amount,
                true,
                TransactionType.DEPOSIT,
                ReferenceType.MANUAL,
                referenceId,
                description
        );
    }

    @Transactional
    public String debit(
            Long userId,
            BigDecimal amount,
            TransactionType txType,
            ReferenceType referenceType,
            Long referenceId,
            String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "So tien tru vi phai lon hon 0.";
        }

        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            return "So du vi khong du de thanh toan.";
        }
        applyWalletMutation(wallet, amount, false, txType, referenceType, referenceId, description);
        return null;
    }

    @Transactional
    public void credit(
            Long userId,
            BigDecimal amount,
            TransactionType txType,
            ReferenceType referenceType,
            Long referenceId,
            String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Wallet wallet = getOrCreateWallet(userId);
        applyWalletMutation(wallet, amount, true, txType, referenceType, referenceId, description);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long walletId) {
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
    }

    private void applyWalletMutation(
            Wallet wallet,
            BigDecimal amount,
            boolean credit,
            TransactionType txType,
            ReferenceType referenceType,
            Long referenceId,
            String description) {
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = credit ? balanceBefore.add(amount) : balanceBefore.subtract(amount);

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setType(txType);
        tx.setAmount(amount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        tx.setDescription(description);
        transactionRepository.save(tx);
    }
}
