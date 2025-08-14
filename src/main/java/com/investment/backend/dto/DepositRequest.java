package com.investment.backend.dto;

import java.math.BigDecimal;

public class DepositRequest {
    private double amount;
    private String currency;  // e.g., "BTC", "ETH", "USDT"
    private String walletAddress;  // User's wallet address for the deposit

    // Constructors
    public DepositRequest() {
    }

    public DepositRequest(double amount, String currency, String walletAddress) {
        this.amount = amount;
        this.currency = currency;
        this.walletAddress = walletAddress;
    }

    // Getters and Setters
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
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
}