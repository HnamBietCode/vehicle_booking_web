package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.SoberBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoberBookingRepository extends JpaRepository<SoberBooking, Long> {

    List<SoberBooking> findByCustomerId(Long customerId);

    List<SoberBooking> findByDriverId(Long driverId);

    @Query("SELECT COUNT(b) FROM SoberBooking b WHERE b.customerId = :customerId AND b.status NOT IN :statuses")
    long countByCustomerIdAndStatusNotIn(@Param("customerId") Long customerId, @Param("statuses") List<SoberBooking.SoberBookingStatus> statuses);

    @Query("SELECT b FROM SoberBooking b WHERE b.status = 'PENDING'")
    List<SoberBooking> findPendingBookings();

    @Query("SELECT COUNT(b) FROM SoberBooking b WHERE b.status = 'PENDING'")
    long countPending();

    List<SoberBooking> findByStatusAndCreatedAtBefore(SoberBooking.SoberBookingStatus status, java.time.LocalDateTime threshold);

    @Query("SELECT COUNT(b) FROM SoberBooking b WHERE b.customerId = :customerId AND b.paymentStatus = 'PENDING' AND b.status != 'CANCELLED'")
    long countUnpaidBookings(@Param("customerId") Long customerId);

    @Modifying
    @Query("UPDATE SoberBooking b SET b.driverId = NULL WHERE b.driverId = :driverId")
    void clearDriverId(@Param("driverId") Long driverId);

    void deleteByCustomerId(Long customerId);

    @Query("SELECT b FROM SoberBooking b WHERE b.driverId = :driverId AND b.status IN ('COMPLETED', 'CANCELLED') ORDER BY b.updatedAt DESC")
    List<SoberBooking> findDriverHistory(@Param("driverId") Long driverId);
}
