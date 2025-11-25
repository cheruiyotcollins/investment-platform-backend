package com.investment.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WithdrawalResponseDTO {

    private Long withdrawalId;
    private BigDecimal amount;
    private String currency;
    private String walletAddress;
    private String status;
    private String transactionHash;
    private LocalDateTime createdAt;

    // Constructors
    public WithdrawalResponseDTO() {}

    public WithdrawalResponseDTO(Long withdrawalId, BigDecimal amount, String currency,
                                 String walletAddress, String status, LocalDateTime createdAt) {
        this.withdrawalId = withdrawalId;
        this.amount = amount;
        this.currency = currency;
        this.walletAddress = walletAddress;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getWithdrawalId() {
        return withdrawalId;
    }

    public void setWithdrawalId(Long withdrawalId) {
        this.withdrawalId = withdrawalId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
