package com.investment.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "investments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // store user relation
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    private Integer planId;
    private String planName;

    private BigDecimal amount;
    private String asset;

    private double dailyReturnPercent;
    private int durationDays;

    private BigDecimal profit; // estimated total profit (not principal)

    @Column(nullable = false)
    private String status; // e.g. active, completed, cancelled

    private OffsetDateTime createdAt;
}
