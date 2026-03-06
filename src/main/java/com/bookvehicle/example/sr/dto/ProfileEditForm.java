package com.bookvehicle.example.sr.dto;

public class ProfileEditForm {

    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
    private String cccd;

    // Getters & Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }
}
