package com.bookvehicle.example.sr.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReportDashboardDTO {
    private BigDecimal totalRevenue;
    private Long totalTrips;
    private List<TopDriverDTO> topDrivers;

    // Getters and Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(Long totalTrips) {
        this.totalTrips = totalTrips;
    }

    public List<TopDriverDTO> getTopDrivers() {
        return topDrivers;
    }

    public void setTopDrivers(List<TopDriverDTO> topDrivers) {
        this.topDrivers = topDrivers;
    }
}
