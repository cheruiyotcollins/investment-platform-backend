package com.investment.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_records")
@Getter
@Setter
public class DepositRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "wallet_address", nullable = false, length = 100)
    private String walletAddress;

    @Column(length = 20)
    private String network; // For USDT (ERC20/TRC20)

    @Column(name = "screenshot_path", length = 255)
    private String screenshotPath;

    @Column(nullable = false, length = 20)
    private String status = "PENDING_VERIFICATION"; // PENDING_VERIFICATION, COMPLETED, REJECTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(length = 500)
    private String remarks;

    // Constructors
    public DepositRecord() {
    }



    @Override
    public String toString() {
        return "DepositRecord{" +
                "id=" + id +
                ", userId=" + userId +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", walletAddress='" + walletAddress + '\'' +
                ", network='" + network + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}