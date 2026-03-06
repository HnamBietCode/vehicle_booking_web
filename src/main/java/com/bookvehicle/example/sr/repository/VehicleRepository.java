package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.Vehicle;
import com.bookvehicle.example.sr.model.VehicleCategory;
import com.bookvehicle.example.sr.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /**
     * Tìm xe khả dụng (AVAILABLE) theo loại + giá/km tối đa + khu vực.
     * Nếu category, maxPrice hoặc location là null -> bỏ qua điều kiện đó.
     */
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

    /**
     * Tìm xe khả dụng có tài xế rảnh + khu vực.
     */
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
}
