package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.walletId IN (SELECT w.id FROM Wallet w WHERE w.userId = :userId)")
    void deleteByWalletUserId(@Param("userId") Long userId);
}
