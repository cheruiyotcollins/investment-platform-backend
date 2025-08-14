package com.investment.backend.entity;

import java.math.BigDecimal;
import java.util.Optional;

public enum InvestmentPlan {
    VENTURE(1, "Venture Plan", 50, 99, 15, 2.0),
    VOYAGER(2, "Voyager Plan", 100, 249, 30, 5.0),
    PIONEER(3, "Pioneer Plan", 250, 499, 45, 7.5),
    CATALYST(4, "Catalyst Plan", 500, 999, 60, 10.0),
    LEGEND(5, "Legend Plan", 1000, 99999, 90, 12.5);

    public final int id;
    public final String name;
    public final BigDecimal minAmount;
    public final BigDecimal maxAmount;
    public final int durationDays;
    public final double dailyReturnPercent;

    InvestmentPlan(int id, String name, int min, int max, int days, double dailyReturn) {
        this.id = id;
        this.name = name;
        this.minAmount = BigDecimal.valueOf(min);
        this.maxAmount = BigDecimal.valueOf(max);
        this.durationDays = days;
        this.dailyReturnPercent = dailyReturn;
    }

    public static Optional<InvestmentPlan> ofId(int id) {
        return java.util.Arrays.stream(values()).filter(p -> p.id == id).findFirst();
    }

    public BigDecimal totalReturnFactor() {
        // percent of principal returned as profit = dailyReturnPercent * durationDays / 100
        return BigDecimal.valueOf(dailyReturnPercent * durationDays / 100.0);
    }
}
