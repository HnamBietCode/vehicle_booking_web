package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.SoberRate;
import com.bookvehicle.example.sr.model.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SoberRateRepository extends JpaRepository<SoberRate, Long> {
    Optional<SoberRate> findByVehicleCategory(VehicleCategory category);
}
