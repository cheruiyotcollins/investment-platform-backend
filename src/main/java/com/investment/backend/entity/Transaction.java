package com.investment.backend.entity;


import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal amount;
    private String currency;
    private String type; // "DEPOSIT", "WITHDRAWAL", etc.
    private String status; // "PENDING", "COMPLETED", "FAILED"
    private LocalDateTime createdAt;
    private String txHash; // Blockchain transaction hash

    // Constructors, getters, setters
    public Transaction() {
        this.createdAt = LocalDateTime.now();
    }

    // Add all getters and setters here
}