package com.bookvehicle.example.sr.dto;

public class DriverActiveTripResponse {
    private boolean hasActiveTrip;
    private String tripType;
    private Long tripId;
    private String status;
    private String pickupAddress;
    private boolean shouldTrackLocation;
    private boolean canStartTrip;
    private boolean canCompleteTrip;

    public static DriverActiveTripResponse empty() {
        DriverActiveTripResponse response = new DriverActiveTripResponse();
        response.setHasActiveTrip(false);
        return response;
    }

    public boolean getHasActiveTrip() {
        return hasActiveTrip;
    }

    public void setHasActiveTrip(boolean hasActiveTrip) {
        this.hasActiveTrip = hasActiveTrip;
    }

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

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public boolean isShouldTrackLocation() {
        return shouldTrackLocation;
    }

    public void setShouldTrackLocation(boolean shouldTrackLocation) {
        this.shouldTrackLocation = shouldTrackLocation;
    }

    public boolean isCanStartTrip() {
        return canStartTrip;
    }

    public void setCanStartTrip(boolean canStartTrip) {
        this.canStartTrip = canStartTrip;
    }

    public boolean isCanCompleteTrip() {
        return canCompleteTrip;
    }

    public void setCanCompleteTrip(boolean canCompleteTrip) {
        this.canCompleteTrip = canCompleteTrip;
    }
}
