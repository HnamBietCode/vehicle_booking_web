package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.ChartDataDTO;
import com.bookvehicle.example.sr.dto.ReportDashboardDTO;
import com.bookvehicle.example.sr.model.Driver;
import com.bookvehicle.example.sr.repository.DriverReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DriverReportService {

    @Autowired
    private DriverReportRepository driverReportRepository;

    public ReportDashboardDTO getDashboardData(Long userId, String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = getStartDateByPeriod(period, now);

        Optional<Driver> driverOpt = driverReportRepository.findByUserId(userId);
        if (driverOpt.isEmpty()) {
            return new ReportDashboardDTO();
        }
        Long driverId = driverOpt.get().getId();

        BigDecimal revenue = driverReportRepository.sumEarningByDateRange(userId, startDate, now);
        if (revenue == null)
            revenue = BigDecimal.ZERO;

        Long trips = driverReportRepository.countTripsByDateRange(driverId, startDate, now);
        if (trips == null)
            trips = 0L;

        ReportDashboardDTO dashboard = new ReportDashboardDTO();
        dashboard.setTotalRevenue(revenue);
        dashboard.setTotalTrips(trips);
        // Note: Top drivers is not needed for individual driver dashboard
        dashboard.setTopDrivers(new ArrayList<>());

        return dashboard;
    }

    public List<ChartDataDTO> getChartData(Long userId, String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = getStartDateByPeriod(period, now);

        Optional<Driver> driverOpt = driverReportRepository.findByUserId(userId);
        if (driverOpt.isEmpty()) {
            return new ArrayList<>();
        }
        Long driverId = driverOpt.get().getId();

        List<Object[]> earningByDate = driverReportRepository.getEarningByDateNative(userId, startDate, now);
        List<Object[]> tripsByDate = driverReportRepository.getTripsByDateNative(driverId, startDate, now);

        Map<LocalDate, BigDecimal> revenueMap = new HashMap<>();
        for (Object[] row : earningByDate) {
            LocalDate date;
            if (row[0] instanceof Date) {
                date = ((Date) row[0]).toLocalDate();
            } else {
                date = LocalDate.parse(row[0].toString());
            }
            BigDecimal rev = new BigDecimal(row[1] != null ? row[1].toString() : "0");
            revenueMap.put(date, rev);
        }

        Map<LocalDate, Long> tripsMap = new HashMap<>();
        for (Object[] row : tripsByDate) {
            LocalDate date;
            if (row[0] instanceof Date) {
                date = ((Date) row[0]).toLocalDate();
            } else {
                date = LocalDate.parse(row[0].toString());
            }
            Long trps = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            tripsMap.put(date, trps);
        }

        List<ChartDataDTO> chartData = new ArrayList<>();
        long daysBetween = ChronoUnit.DAYS.between(startDate.toLocalDate(), now.toLocalDate());
        for (int i = 0; i <= daysBetween; i++) {
            LocalDate currentDate = startDate.toLocalDate().plusDays(i);
            BigDecimal currentRev = revenueMap.getOrDefault(currentDate, BigDecimal.ZERO);
            Long currentTrips = tripsMap.getOrDefault(currentDate, 0L);
            chartData.add(new ChartDataDTO(currentDate, currentRev, currentTrips));
        }

        return chartData;
    }

    private LocalDateTime getStartDateByPeriod(String period, LocalDateTime now) {
        if ("weekly".equalsIgnoreCase(period)) {
            return now.minusDays(7);
        } else if ("monthly".equalsIgnoreCase(period)) {
            return now.minusMonths(1);
        } else if ("yearly".equalsIgnoreCase(period)) {
            return now.minusYears(1);
        }
        return now.minusDays(30);
    }
}
