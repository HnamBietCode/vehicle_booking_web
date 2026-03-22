package com.bookvehicle.example.sr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ChartDataDTO {
    private LocalDate date;
    private BigDecimal revenue;
    private Long trips;

    public ChartDataDTO(LocalDate date, BigDecimal revenue, Long trips) {
        this.date = date;
        this.revenue = revenue;
        this.trips = trips;
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public Long getTrips() {
        return trips;
    }

    public void setTrips(Long trips) {
        this.trips = trips;
    }
}
