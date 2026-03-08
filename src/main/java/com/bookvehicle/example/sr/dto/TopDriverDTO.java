package com.bookvehicle.example.sr.dto;

import java.math.BigDecimal;

public class TopDriverDTO {
    private Long id;
    private String name;
    private Long totalTrips;
    private BigDecimal rating;

    public TopDriverDTO(Long id, String name, Long totalTrips, BigDecimal rating) {
        this.id = id;
        this.name = name;
        this.totalTrips = totalTrips;
        this.rating = rating;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(Long totalTrips) {
        this.totalTrips = totalTrips;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }
}
