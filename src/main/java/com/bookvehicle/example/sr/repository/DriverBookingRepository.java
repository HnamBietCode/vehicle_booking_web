package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.DriverBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverBookingRepository extends JpaRepository<DriverBooking, Long> {

    List<DriverBooking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<DriverBooking> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    @Query("SELECT b FROM DriverBooking b WHERE b.status = 'FINDING_DRIVER'")
    List<DriverBooking> findPendingBookings();

    @Modifying
    @Query("UPDATE DriverBooking b SET b.driverId = NULL WHERE b.driverId = :driverId")
    void clearDriverId(@Param("driverId") Long driverId);

    void deleteByCustomerId(Long customerId);
}
