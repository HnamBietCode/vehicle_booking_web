package com.bookvehicle.example.sr.dto;

public class RegisterForm {

    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String confirmPassword;
    /** "CUSTOMER" hoặc "DRIVER" */
    private String role = "CUSTOMER";
    private String cccd;

    // Getters & Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }
}
