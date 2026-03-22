package com.bookvehicle.example.sr.dto;

import java.math.BigDecimal;

public class RentalCompleteForm {
    private BigDecimal extraFee = BigDecimal.ZERO;
    private String notes;

    public BigDecimal getExtraFee() {
        return extraFee;
    }

    public void setExtraFee(BigDecimal extraFee) {
        this.extraFee = extraFee;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
