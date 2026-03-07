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

    /**
     * Get user's wallet. Create one if it doesn't exist yet.
     */
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

    /**
     * Deposit amount to user's wallet and log the transaction
     */
    @Transactional
    public void deposit(Long userId, BigDecimal amount, String description, Long referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }

        Wallet wallet = getOrCreateWallet(userId);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        // Update Wallet Balance
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        // Update Transactions log
        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(amount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setReferenceType(ReferenceType.MANUAL);
        tx.setReferenceId(referenceId);
        tx.setDescription(description);

        transactionRepository.save(tx);
    }

    /**
     * Get Transaction History for a wallet
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long walletId) {
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
    }
}
