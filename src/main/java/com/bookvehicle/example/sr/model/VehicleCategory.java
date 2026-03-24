package com.bookvehicle.example.sr.model;

public enum VehicleCategory {
    MOTORCYCLE("Xe máy"),
    CAR_4("Xe 4 chỗ"),
    CAR_7("Xe 7 chỗ"),
    CAR_16("Xe 16 chỗ"),
    CAR_29("Xe 29 chỗ"),
    CAR_45("Xe 45 chỗ"),
    TRUCK_SMALL("Xe tải nhỏ"),
    TRUCK_MEDIUM("Xe tải trung"),
    TRUCK_LARGE("Xe tải lớn");

    private final String displayName;

    VehicleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
