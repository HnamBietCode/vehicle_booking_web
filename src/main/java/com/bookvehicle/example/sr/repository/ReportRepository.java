package com.bookvehicle.example.sr.repository;

import com.bookvehicle.example.sr.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Driver, Long> {

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type IN ('SYSTEM_FEE', 'BOOKING_PAYMENT', 'RENTAL_PAYMENT') AND created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT " +
            "(SELECT COUNT(*) FROM vehicle_rentals WHERE status = 'COMPLETED' AND created_at BETWEEN :startDate AND :endDate) + "
            +
            "(SELECT COUNT(*) FROM driver_bookings WHERE status = 'COMPLETED' AND created_at BETWEEN :startDate AND :endDate)", nativeQuery = true)
    Long countTripsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT " +
            "d.id, " +
            "d.full_name, " +
            "COALESCE(trips.total_trips, 0) as totalTrips, " +
            "COALESCE(v.avg_rating, 0.0) as rating " +
            "FROM drivers d " +
            "LEFT JOIN vehicles v ON d.id = v.assigned_driver " +
            "LEFT JOIN ( " +
            "  SELECT driver_id, SUM(cnt) as total_trips FROM ( " +
            "    SELECT driver_id, COUNT(*) as cnt FROM vehicle_rentals WHERE status = 'COMPLETED' GROUP BY driver_id " +
            "    UNION ALL " +
            "    SELECT driver_id, COUNT(*) as cnt FROM driver_bookings WHERE status = 'COMPLETED' GROUP BY driver_id " +
            "  ) combined GROUP BY driver_id " +
            ") trips ON d.id = trips.driver_id " +
            "ORDER BY totalTrips DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopDriversNative(@Param("limit") int limit);

    @Query(value = "SELECT report_date as date, total_revenue as revenue, (total_rentals + total_bookings) as trips " +
            "FROM daily_revenue " +
            "WHERE report_date BETWEEN :startDate AND :endDate " +
            "ORDER BY report_date ASC", nativeQuery = true)
    List<Object[]> getDailyRevenueAndTripsNative(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // In case daily_revenue table is empty, we can fallback to aggregate
    // transactions, but assuming daily_revenue is populated for simplicity or we
    // aggregate on the fly:
    @Query(value = "SELECT DATE(created_at) as date, COALESCE(SUM(amount), 0) as revenue " +
            "FROM transactions " +
            "WHERE type IN ('SYSTEM_FEE', 'BOOKING_PAYMENT', 'RENTAL_PAYMENT') " +
            "AND created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at) ASC", nativeQuery = true)
    List<Object[]> getRevenueByDateNative(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT dt, SUM(trips) as trips FROM ( " +
            "SELECT DATE(created_at) as dt, COUNT(*) as trips FROM vehicle_rentals WHERE status = 'COMPLETED' AND created_at BETWEEN :startDate AND :endDate GROUP BY DATE(created_at) "
            +
            "UNION ALL " +
            "SELECT DATE(created_at) as dt, COUNT(*) as trips FROM driver_bookings WHERE status = 'COMPLETED' AND created_at BETWEEN :startDate AND :endDate GROUP BY DATE(created_at) "
            +
            ") as combined " +
            "GROUP BY dt ORDER BY dt ASC", nativeQuery = true)
    List<Object[]> getTripsByDateNative(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
