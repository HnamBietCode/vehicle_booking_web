package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverReportRepository extends JpaRepository<Driver, Long> {

    @Query("SELECT d FROM Driver d WHERE d.user.id = :userId")
    Optional<Driver> findByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM transactions t " +
            "JOIN wallets w ON t.wallet_id = w.id " +
            "WHERE w.user_id = :userId " +
            "AND type = 'DRIVER_EARNING' " +
            "AND t.created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    BigDecimal sumEarningByDateRange(@Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT " +
            "(SELECT COUNT(*) FROM vehicle_rentals WHERE status = 'COMPLETED' AND driver_id = :driverId AND created_at BETWEEN :startDate AND :endDate) + "
            +
            "(SELECT COUNT(*) FROM driver_bookings WHERE status = 'COMPLETED' AND driver_id = :driverId AND created_at BETWEEN :startDate AND :endDate)", nativeQuery = true)
    Long countTripsByDateRange(@Param("driverId") Long driverId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT DATE(t.created_at) as date, COALESCE(SUM(t.amount), 0) as revenue " +
            "FROM transactions t " +
            "JOIN wallets w ON t.wallet_id = w.id " +
            "WHERE w.user_id = :userId " +
            "AND t.type = 'DRIVER_EARNING' " +
            "AND t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(t.created_at) " +
            "ORDER BY DATE(t.created_at) ASC", nativeQuery = true)
    List<Object[]> getEarningByDateNative(@Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT dt, SUM(trips) as trips FROM ( " +
            "SELECT DATE(created_at) as dt, COUNT(*) as trips FROM vehicle_rentals WHERE status = 'COMPLETED' AND driver_id = :driverId AND created_at BETWEEN :startDate AND :endDate GROUP BY DATE(created_at) "
            +
            "UNION ALL " +
            "SELECT DATE(created_at) as dt, COUNT(*) as trips FROM driver_bookings WHERE status = 'COMPLETED' AND driver_id = :driverId AND created_at BETWEEN :startDate AND :endDate GROUP BY DATE(created_at) "
            +
            ") as combined " +
            "GROUP BY dt ORDER BY dt ASC", nativeQuery = true)
    List<Object[]> getTripsByDateNative(@Param("driverId") Long driverId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
