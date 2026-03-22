package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUserId(Long userId);
    boolean existsByCccd(String cccd);

    @Query("SELECT d FROM Driver d WHERE d.isAvailable = true AND d.verificationStatus = com.bookvehicle.example.sr.model.VerificationStatus.APPROVED AND d.vehicleTypes LIKE %:vehicleType%")
    List<Driver> findAvailableByVehicleType(@Param("vehicleType") String vehicleType);

    @Query("SELECT COUNT(d) FROM Driver d WHERE d.isAvailable = true AND d.verificationStatus = com.bookvehicle.example.sr.model.VerificationStatus.APPROVED")
    long countOnlineApproved();
}
