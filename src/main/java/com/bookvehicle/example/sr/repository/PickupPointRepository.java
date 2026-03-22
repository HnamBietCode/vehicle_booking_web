package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.PickupPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PickupPointRepository extends JpaRepository<PickupPoint, Long> {
    List<PickupPoint> findByIsActiveTrueOrderByNameAsc();
    Optional<PickupPoint> findByIdAndIsActiveTrue(Long id);
    boolean existsByNameIgnoreCaseAndAddressIgnoreCase(String name, String address);
    boolean existsByNameIgnoreCaseAndAddressIgnoreCaseAndIdNot(String name, String address, Long id);

    @Query(value = "SELECT p.* FROM pickup_points p " +
            "JOIN (" +
            "   SELECT MIN(id) AS id " +
            "   FROM pickup_points " +
            "   WHERE is_active = TRUE " +
            "   GROUP BY address" +
            ") x ON p.id = x.id " +
            "ORDER BY p.name", nativeQuery = true)
    List<PickupPoint> findDistinctActivePickupPoints();
}
