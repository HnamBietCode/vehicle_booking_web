package com.bookvehicle.example.sr.dto;

import com.bookvehicle.example.sr.model.VehicleRental;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class RentalCreateForm {
    private Long vehicleId;
    private Long pickupPointId;
    private String pickupAddress;
    private VehicleRental.RentalType rentalType = VehicleRental.RentalType.HOURLY;
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
