package com.bookvehicle.example.sr.service;

import com.bookvehicle.example.sr.dto.ChartDataDTO;
import com.bookvehicle.example.sr.dto.ReportDashboardDTO;
import com.bookvehicle.example.sr.dto.TopDriverDTO;
import com.bookvehicle.example.sr.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    public ReportDashboardDTO getDashboardData(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("weekly".equalsIgnoreCase(period)) {
            startDate = now.minusDays(7);
        } else if ("monthly".equalsIgnoreCase(period)) {
            startDate = now.minusMonths(1);
        } else if ("yearly".equalsIgnoreCase(period)) {
            startDate = now.minusYears(1);
        } else {
            // Default 30 days
            startDate = now.minusDays(30);
        }

        BigDecimal revenue = reportRepository.sumRevenueByDateRange(startDate, now);
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }

        Long trips = reportRepository.countTripsByDateRange(startDate, now);
        if (trips == null) {
            trips = 0L;
        }

        List<Object[]> topDriverResults = reportRepository.findTopDriversNative(5);
        List<TopDriverDTO> topDrivers = new ArrayList<>();

        for (Object[] row : topDriverResults) {
            Long id = ((Number) row[0]).longValue();
            String name = (String) row[1];
            Long totalTrips = ((Number) row[2]).longValue();
            BigDecimal rating = new BigDecimal(row[3].toString());
            topDrivers.add(new TopDriverDTO(id, name, totalTrips, rating));
        }

        ReportDashboardDTO dashboard = new ReportDashboardDTO();
        dashboard.setTotalRevenue(revenue);
        dashboard.setTotalTrips(trips);
        dashboard.setTopDrivers(topDrivers);

        return dashboard;
    }

    public List<ChartDataDTO> getChartData(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        if ("weekly".equalsIgnoreCase(period)) {
            startDate = now.minusDays(7);
        } else if ("monthly".equalsIgnoreCase(period)) {
            startDate = now.minusMonths(1);
        } else if ("yearly".equalsIgnoreCase(period)) {
            startDate = now.minusYears(1);
        } else {
            // Default 30 days
            startDate = now.minusDays(30);
        }

        List<Object[]> revenueByDate = reportRepository.getRevenueByDateNative(startDate, now);
        List<Object[]> tripsByDate = reportRepository.getTripsByDateNative(startDate, now);

        Map<LocalDate, BigDecimal> revenueMap = new HashMap<>();
        for (Object[] row : revenueByDate) {
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

        // Fill gaps
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
}
