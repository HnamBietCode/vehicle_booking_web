package com.bookvehicle.example.sr.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sober_rates")
public class SoberRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category", nullable = false, unique = true)
    private VehicleCategory vehicleCategory;

    @Column(name = "hourly_rate", nullable = false)
    private BigDecimal hourlyRate;

    @Column(name = "daily_rate", nullable = false)
    private BigDecimal dailyRate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VehicleCategory getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(VehicleCategory vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
