package com.investment.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentResponse {
    private Long id;
    private OffsetDateTime createdAt;
    private Integer planId;
    private String planName;
    private BigDecimal amount;
    private String asset;
    private String status;
    private BigDecimal profit;
    private double dailyReturnPercent;
    private int durationDays;
}
