package com.bookvehicle.example.sr.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sober_bookings")
public class SoberBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "driver_id")
    private Long driverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category", nullable = false)
    private VehicleCategory vehicleCategory;

    @Column(name = "customer_vehicle", length = 100)
    private String customerVehicle;

    @Column(name = "pickup_address", nullable = false, length = 300)
    private String pickupAddress;

    @Column(length = 100)
    private String province;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String ward;

    @Column(name = "pickup_lat")
    private Double pickupLat;

    @Column(name = "pickup_lng")
    private Double pickupLng;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false)
    private DurationUnit durationUnit;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SoberBookingStatus status = SoberBookingStatus.PENDING;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "daily_rate", precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Convenience Joins
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", insertable = false, updatable = false)
    private Driver driver;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DurationUnit {
        HOURLY, DAILY
    }

    public enum SoberBookingStatus {
        PENDING, ACCEPTED, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public VehicleCategory getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(VehicleCategory vehicleCategory) { this.vehicleCategory = vehicleCategory; }
    public String getCustomerVehicle() { return customerVehicle; }
    public void setCustomerVehicle(String customerVehicle) { this.customerVehicle = customerVehicle; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public Double getPickupLat() { return pickupLat; }
    public void setPickupLat(Double pickupLat) { this.pickupLat = pickupLat; }
    public Double getPickupLng() { return pickupLng; }
    public void setPickupLng(Double pickupLng) { this.pickupLng = pickupLng; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public DurationUnit getDurationUnit() { return durationUnit; }
    public void setDurationUnit(DurationUnit durationUnit) { this.durationUnit = durationUnit; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public SoberBookingStatus getStatus() { return status; }
    public void setStatus(SoberBookingStatus status) { this.status = status; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public LocalDateTime getActualStart() { return actualStart; }
    public void setActualStart(LocalDateTime actualStart) { this.actualStart = actualStart; }
    public LocalDateTime getActualEnd() { return actualEnd; }
    public void setActualEnd(LocalDateTime actualEnd) { this.actualEnd = actualEnd; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }
}
