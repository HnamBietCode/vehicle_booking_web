package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.WithdrawalRequest;
import com.bookvehicle.example.sr.model.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WithdrawalRequest> findByStatusOrderByCreatedAtAsc(WithdrawalStatus status);
    List<WithdrawalRequest> findAllByOrderByCreatedAtDesc();
    void deleteByUserId(Long userId);
}
