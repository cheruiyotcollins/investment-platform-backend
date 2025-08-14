package com.investment.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
@Entity
@Table(name = "referrals")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Referral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_person_id", nullable = false)
    private User referredPerson;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime referralDate = LocalDateTime.now();

    @Column(nullable = false)
    private boolean isPaid = false; // The person who was referred
         // Whether the referral has been paid out

    // Constructor
    public Referral(User referrer, User referredPerson, BigDecimal amount) {
        this.referrer = referrer;
        this.referredPerson = referredPerson;
        this.amount = amount;
        this.referralDate = LocalDateTime.now();
        this.isPaid = false;
    }

    // Getters and Setters


    // Utility methods
    public void markAsPaid() {
        this.isPaid = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Referral referral = (Referral) o;
        return amount.compareTo(referral.amount) == 0 &&
                isPaid == referral.isPaid &&
                Objects.equals(referrer, referral.referrer) &&
                Objects.equals(referredPerson, referral.referredPerson) &&
                Objects.equals(referralDate, referral.referralDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referrer, referredPerson, amount, referralDate, isPaid);
    }

    @Override
    public String toString() {
        return "Referral{" +
                "referrer=" + referrer +
                ", referredPerson=" + referredPerson +
                ", amount=" + amount +
                ", referralDate=" + referralDate +
                ", isPaid=" + isPaid +
                '}';
    }
}