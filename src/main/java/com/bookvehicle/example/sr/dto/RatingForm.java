package com.bookvehicle.example.sr.dto;

public class RatingForm {

    private Integer stars;       // 1-5
    private String comment;
    private String targetType;   // DRIVER / VEHICLE
    private Long targetId;
    private String refType;      // RENTAL / BOOKING
    private Long refId;

    // Optional fields for rating the driver at the same time as the vehicle (or vice versa)
    private Integer driverStars;
    private String driverComment;
    private Integer vehicleStars;
    private String vehicleComment;

    // ─── Getters & Setters ────────────────────────────────────────

    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }

    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }

    public Integer getDriverStars() { return driverStars; }
    public void setDriverStars(Integer driverStars) { this.driverStars = driverStars; }

    public String getDriverComment() { return driverComment; }
    public void setDriverComment(String driverComment) { this.driverComment = driverComment; }

    public Integer getVehicleStars() { return vehicleStars; }
    public void setVehicleStars(Integer vehicleStars) { this.vehicleStars = vehicleStars; }

    public String getVehicleComment() { return vehicleComment; }
    public void setVehicleComment(String vehicleComment) { this.vehicleComment = vehicleComment; }
}
