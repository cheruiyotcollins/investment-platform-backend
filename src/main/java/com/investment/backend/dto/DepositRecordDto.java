package com.investment.backend.dto;


import com.investment.backend.entity.DepositRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DepositRecordDto {
    private Long id;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String network;

    // Constructor from Entity
    public void DepositRecordDTO(DepositRecord record) {
        this.id = record.getId();
        this.amount = record.getAmount();
        this.currency = record.getCurrency();
        this.status = record.getStatus();
        this.createdAt = record.getCreatedAt();
        this.processedAt = record.getProcessedAt();
        this.network = record.getNetwork();
    }

    // Getters
    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public String getNetwork() { return network; }
}
