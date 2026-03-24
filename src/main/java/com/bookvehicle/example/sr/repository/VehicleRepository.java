package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.Vehicle;
import com.bookvehicle.example.sr.model.VehicleCategory;
import com.bookvehicle.example.sr.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByCategory(VehicleCategory category);

    List<Vehicle> findByStatus(VehicleStatus status);

    List<Vehicle> findByCategoryAndStatus(VehicleCategory category, VehicleStatus status);

    Optional<Vehicle> findByAssignedDriver(Long driverId);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);

    @Query("SELECT v FROM Vehicle v WHERE v.status = 'AVAILABLE' " +
           "AND (:category IS NULL OR v.category = :category) " +
           "AND (:maxPricePerKm IS NULL OR v.pricePerKm <= :maxPricePerKm) " +
           "AND (:location IS NULL OR :location = '' OR LOWER(v.currentAddress) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "ORDER BY v.avgRating DESC, v.pricePerKm ASC")
    List<Vehicle> findAvailable(
            @Param("category") VehicleCategory category,
            @Param("maxPricePerKm") BigDecimal maxPricePerKm,
            @Param("location") String location
    );

    @Query("SELECT v FROM Vehicle v JOIN v.driver d WHERE v.status = 'AVAILABLE' " +
           "AND d.isAvailable = true AND d.verificationStatus = 'APPROVED' " +
           "AND (:category IS NULL OR v.category = :category) " +
           "AND (:maxPricePerKm IS NULL OR v.pricePerKm <= :maxPricePerKm) " +
           "AND (:location IS NULL OR :location = '' OR LOWER(v.currentAddress) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "ORDER BY v.avgRating DESC, v.pricePerKm ASC")
    List<Vehicle> findAvailableWithReadyDriver(
            @Param("category") VehicleCategory category,
            @Param("maxPricePerKm") BigDecimal maxPricePerKm,
            @Param("location") String location
    );

    @Query("SELECT v FROM Vehicle v WHERE (:category IS NULL OR v.category = :category) " +
           "AND (:maxPricePerKm IS NULL OR v.pricePerKm <= :maxPricePerKm) " +
           "AND (:location IS NULL OR :location = '' OR LOWER(v.currentAddress) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "ORDER BY CASE WHEN v.status = 'AVAILABLE' THEN 0 ELSE 1 END, v.avgRating DESC, v.pricePerKm ASC")
    List<Vehicle> searchAll(
            @Param("category") VehicleCategory category,
            @Param("maxPricePerKm") BigDecimal maxPricePerKm,
            @Param("location") String location
    );

    @Query("SELECT v FROM Vehicle v LEFT JOIN v.driver d WHERE " +
           "(:category IS NULL OR v.category = :category) " +
           "AND (:maxPricePerKm IS NULL OR v.pricePerKm <= :maxPricePerKm) " +
           "AND (:location IS NULL OR :location = '' OR LOWER(v.currentAddress) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (v.driver IS NOT NULL AND d.verificationStatus = 'APPROVED') " +
           "ORDER BY CASE WHEN v.status = 'AVAILABLE' THEN 0 ELSE 1 END, v.avgRating DESC, v.pricePerKm ASC")
    List<Vehicle> searchAllWithDriverFilters(
            @Param("category") VehicleCategory category,
            @Param("maxPricePerKm") BigDecimal maxPricePerKm,
            @Param("location") String location
    );

    @Modifying
    @Query("UPDATE Vehicle v SET v.assignedDriver = NULL WHERE v.assignedDriver = :driverId")
    void clearAssignedDriver(@Param("driverId") Long driverId);
}
