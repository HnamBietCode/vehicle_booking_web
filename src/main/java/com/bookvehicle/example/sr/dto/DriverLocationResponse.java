package com.bookvehicle.example.sr.dto;

import java.time.Instant;

public class DriverLocationResponse {
    private String tripType;
    private Long tripId;
    private Double lat;
    private Double lng;
    private Double speed;
    private Double bearing;
    private Double accuracy;
    private Instant recordedAt;

    public String getTripType() {
        return tripType;
    }

    public void setTripType(String tripType) {
        this.tripType = tripType;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getBearing() {
        return bearing;
    }

    public void setBearing(Double bearing) {
        this.bearing = bearing;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
