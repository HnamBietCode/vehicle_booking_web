package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.DriverLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DriverLicenseRepository extends JpaRepository<DriverLicense, Long> {
    List<DriverLicense> findByDriverId(Long driverId);

    @Modifying
    @Transactional
    void deleteByDriverId(Long driverId);
}
