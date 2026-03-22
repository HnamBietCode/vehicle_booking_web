package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    @Query("SELECT s FROM UserSession s WHERE s.tokenHash = :tokenHash AND s.expiresAt > :now")
    Optional<UserSession> findValidByTokenHash(@Param("tokenHash") String tokenHash,
                                               @Param("now") LocalDateTime now);

    void deleteByTokenHash(String tokenHash);

    void deleteByUserId(Long userId);
}
