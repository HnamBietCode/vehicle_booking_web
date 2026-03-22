package com.bookvehicle.example.sr.dto;

import com.bookvehicle.example.sr.model.VehicleRental;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class RentalCreateForm {
    private Long vehicleId;
    private Long pickupPointId;
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private VehicleRental.RentalType rentalType = VehicleRental.RentalType.HOURLY;
    private VehicleRental.RentalMode rentalMode = VehicleRental.RentalMode.WITH_DRIVER;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime plannedStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime plannedEnd;
    private String notes;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Double getPickupLat() {
        return pickupLat;
    }

    public void setPickupLat(Double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public Double getPickupLng() {
        return pickupLng;
    }

    public void setPickupLng(Double pickupLng) {
        this.pickupLng = pickupLng;
    }

    public Long getPickupPointId() {
        return pickupPointId;
    }

    public void setPickupPointId(Long pickupPointId) {
        this.pickupPointId = pickupPointId;
    }

    public VehicleRental.RentalType getRentalType() {
        return rentalType;
    }

    public void setRentalType(VehicleRental.RentalType rentalType) {
        this.rentalType = rentalType;
    }

    public VehicleRental.RentalMode getRentalMode() {
        return rentalMode;
    }

    public void setRentalMode(VehicleRental.RentalMode rentalMode) {
        this.rentalMode = rentalMode;
    }

    public LocalDateTime getPlannedStart() {
        return plannedStart;
    }

    public void setPlannedStart(LocalDateTime plannedStart) {
        this.plannedStart = plannedStart;
    }

    public LocalDateTime getPlannedEnd() {
        return plannedEnd;
    }

    public void setPlannedEnd(LocalDateTime plannedEnd) {
        this.plannedEnd = plannedEnd;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
