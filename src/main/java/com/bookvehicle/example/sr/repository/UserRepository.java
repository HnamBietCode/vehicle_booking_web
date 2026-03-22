package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findFirstByRole(Role role);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
