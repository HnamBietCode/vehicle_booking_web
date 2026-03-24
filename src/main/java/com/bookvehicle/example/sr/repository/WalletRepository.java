package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
