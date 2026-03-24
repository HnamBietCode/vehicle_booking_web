package com.bookvehicle.example.sr.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('STANDARD','PREMIUM') DEFAULT 'STANDARD'")
    private MembershipType membership = MembershipType.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "premium_tier", columnDefinition = "ENUM('BRONZE','SILVER','GOLD')")
    private PremiumTier premiumTier;

    @Column(name = "premium_exp")
    private LocalDate premiumExp;

    // Convenience join – NOT a mapped FK column, loaded on demand
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

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public MembershipType getMembership() { return membership; }
    public void setMembership(MembershipType membership) { this.membership = membership; }

    public PremiumTier getPremiumTier() { return premiumTier; }
    public void setPremiumTier(PremiumTier premiumTier) { this.premiumTier = premiumTier; }

    public LocalDate getPremiumExp() { return premiumExp; }
    public void setPremiumExp(LocalDate premiumExp) { this.premiumExp = premiumExp; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @Transient
    public boolean isPremiumActive() {
        return membership == MembershipType.PREMIUM
                && premiumTier != null
                && premiumExp != null
                && !premiumExp.isBefore(LocalDate.now());
    }

    @Transient
    public String getPremiumTierDisplayName() {
        return premiumTier != null ? premiumTier.getDisplayName() : null;
    }
}
