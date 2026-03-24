package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.VehicleRental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VehicleRentalRepository extends JpaRepository<VehicleRental, Long> {

    @Query("SELECT COUNT(vr) > 0 FROM VehicleRental vr " +
           "WHERE vr.vehicleId = :vehicleId " +
           "AND vr.status IN :activeStatuses " +
           "AND vr.plannedStart < :plannedEnd " +
           "AND vr.plannedEnd > :plannedStart")
    boolean hasVehicleOverlap(
            @Param("vehicleId") Long vehicleId,
            @Param("plannedStart") LocalDateTime plannedStart,
            @Param("plannedEnd") LocalDateTime plannedEnd,
            @Param("activeStatuses") List<VehicleRental.RentalStatus> activeStatuses
    );

    @Query("SELECT COUNT(vr) > 0 FROM VehicleRental vr " +
           "WHERE vr.driverId = :driverId " +
           "AND vr.status IN :activeStatuses " +
           "AND vr.plannedStart < :plannedEnd " +
           "AND vr.plannedEnd > :plannedStart")
    boolean hasDriverOverlap(
            @Param("driverId") Long driverId,
            @Param("plannedStart") LocalDateTime plannedStart,
            @Param("plannedEnd") LocalDateTime plannedEnd,
            @Param("activeStatuses") List<VehicleRental.RentalStatus> activeStatuses
    );

    List<VehicleRental> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<VehicleRental> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    @Query("SELECT vr FROM VehicleRental vr " +
           "WHERE vr.status = 'PENDING' " +
           "AND vr.rentalMode = 'VEHICLE_ONLY' " +
           "AND vr.driverId IS NULL")
    List<VehicleRental> findPendingVehicleOnly();

    @Query("SELECT COUNT(vr) FROM VehicleRental vr " +
           "WHERE vr.status = 'PENDING' " +
           "AND vr.rentalMode = 'VEHICLE_ONLY' " +
           "AND vr.driverId IS NULL")
    long countPendingVehicleOnly();

    @Query("SELECT vr FROM VehicleRental vr " +
           "WHERE vr.status = 'PENDING'")
    List<VehicleRental> findAllPending();

    @Modifying
    @Query("UPDATE VehicleRental vr SET vr.driverId = NULL WHERE vr.driverId = :driverId")
    void clearDriverId(@Param("driverId") Long driverId);

    void deleteByCustomerId(Long customerId);
}
