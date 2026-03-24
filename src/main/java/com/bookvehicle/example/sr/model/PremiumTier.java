package com.bookvehicle.example.sr.model;

import java.math.BigDecimal;
import java.util.List;

public enum PremiumTier {
    BRONZE(
            "Dong",
            new BigDecimal("99000"),
            "Khoi dau uu tien cho khach hang dat xe thuong xuyen.",
            List.of(
                    "Tich diem (Moi chuyen 10 diem, 100 diem = giam 10k)",
                    "Uu tien tim tai xe o gan",
                    "3 voucher giam gia tu 5-15k/thang",
                    "Badge nhan dien rank Dong"
            )
    ),
    SILVER(
            "Bac",
            new BigDecimal("199000"),
            "Tang cap uu tien, matching tot hon va doi tai xe linh hoat.",
            List.of(
                    "Uu tien bat chuyen (de chuyen lay do uu tien truoc rank Dong)",
                    "Matching voi tai xe co danh gia >= 4.5 sao",
                    "5 voucher giam tu 15k-30k/thang",
                    "Cho phep doi tai xe 1 lan/thang",
                    "Badge nhan dien rank Bac"
            )
    ),
    GOLD(
            "Vang",
            new BigDecimal("299000"),
            "Goi VIP voi uu tien cao nhat va trai nghiem dich vu tot nhat.",
            List.of(
                    "Chon tai xe yeu thich",
                    "Dam bao matching duoc chuyen la 100% (VIP > Bac > Dong > Normal)",
                    "Matching tai xe co 5 sao, khong nho hon 4.9",
                    "Neu bi huy chuyen hoac tai xe toi tre, duoc tang voucher",
                    "Dich vu dac biet la xe thi se xin hon dong moi nhat",
                    "Uu tien duoc tim tai xe moi khi tai xe cu huy chuyen",
                    "10 voucher giam tu 30-50k/thang",
                    "Badge nhan dien rank Vang"
            )
    );

    private final String displayName;
    private final BigDecimal price;
    private final String summary;
    private final List<String> benefits;

    PremiumTier(String displayName, BigDecimal price, String summary, List<String> benefits) {
        this.displayName = displayName;
        this.price = price;
        this.summary = summary;
        this.benefits = benefits;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getBenefits() {
        return benefits;
    }

    public String getLevelLabel() {
        return "Hang " + displayName;
    }

    public boolean isVip() {
        return this == GOLD;
    }
}
