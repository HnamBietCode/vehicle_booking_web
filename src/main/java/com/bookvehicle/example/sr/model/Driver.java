package com.bookvehicle.example.sr.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 12)
    private String cccd;

    @Column(name = "cccd_image_url", length = 500)
    private String cccdImageUrl;

    @Column(name = "cccd_back_url", length = 500)
    private String cccdBackUrl;

    @Column(name = "driver_license", nullable = false, unique = true, length = 12)
    private String driverLicense;

    @Column(name = "license_image_url", length = 500)
    private String licenseImageUrl;

    @Column(name = "license_expiry", nullable = false)
    private LocalDate licenseExpiry;

    @Column(name = "vehicle_types", nullable = false, length = 50)
    private String vehicleTypes; // e.g. "MOTORCYCLE,CAR_4"

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false,
            columnDefinition = "ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING'")
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(length = 100)
    private String province;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String ward;

    @Column(name = "last_completed_at")
    private LocalDateTime lastCompletedAt;

    // Convenience join
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // ─── Getters & Setters ───────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }

    public String getCccdImageUrl() { return cccdImageUrl; }
    public void setCccdImageUrl(String cccdImageUrl) { this.cccdImageUrl = cccdImageUrl; }

    public String getCccdBackUrl() { return cccdBackUrl; }
    public void setCccdBackUrl(String cccdBackUrl) { this.cccdBackUrl = cccdBackUrl; }

    public String getDriverLicense() { return driverLicense; }
    public void setDriverLicense(String driverLicense) { this.driverLicense = driverLicense; }

    public String getLicenseImageUrl() { return licenseImageUrl; }
    public void setLicenseImageUrl(String licenseImageUrl) { this.licenseImageUrl = licenseImageUrl; }

    public LocalDate getLicenseExpiry() { return licenseExpiry; }
    public void setLicenseExpiry(LocalDate licenseExpiry) { this.licenseExpiry = licenseExpiry; }

    public String getVehicleTypes() { return vehicleTypes; }
    public void setVehicleTypes(String vehicleTypes) { this.vehicleTypes = vehicleTypes; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public LocalDateTime getLastCompletedAt() { return lastCompletedAt; }
    public void setLastCompletedAt(LocalDateTime lastCompletedAt) { this.lastCompletedAt = lastCompletedAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
