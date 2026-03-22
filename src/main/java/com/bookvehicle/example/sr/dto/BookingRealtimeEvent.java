package com.bookvehicle.example.sr.dto;

public class BookingRealtimeEvent {
    private String tripType;
    private Long tripId;
    private String status;
    private String eventType;
    private String message;
    private String trackingUrl;
    private boolean locationAvailable;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }

    public boolean isLocationAvailable() {
        return locationAvailable;
    }

    public void setLocationAvailable(boolean locationAvailable) {
        this.locationAvailable = locationAvailable;
    }
}
