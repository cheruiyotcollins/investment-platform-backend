package com.investment.backend.service;

import com.investment.backend.entity.InvestmentPlan;
import com.investment.backend.dto.InvestRequest;
import com.investment.backend.dto.InvestmentResponse;
import com.investment.backend.entity.Investment;
import com.investment.backend.entity.User;
import com.investment.backend.repository.InvestmentRepository;
import com.investment.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvestmentService {

    private final UserRepository userRepository;
    private final InvestmentRepository investmentRepository;

    public InvestmentService(UserRepository userRepository, InvestmentRepository investmentRepository) {
        this.userRepository = userRepository;
        this.investmentRepository = investmentRepository;
    }

    /**
     * Create an investment. Expects an authenticated principal; adapt if you use custom principal.
     */
    @Transactional
    public InvestmentResponse invest(String username, InvestRequest req) {
        if (req.getPlanId() == null || req.getAmount() == null) {
            throw new IllegalArgumentException("planId and amount are required");
        }

        InvestmentPlan plan = InvestmentPlan.ofId(req.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid planId"));

        BigDecimal amount = req.getAmount();

        if (amount.compareTo(plan.minAmount) < 0 || amount.compareTo(plan.maxAmount) > 0) {
            throw new IllegalArgumentException(String.format("Amount must be between %s and %s", plan.minAmount, plan.maxAmount));
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // attempt atomic deduction
        int updated = userRepository.deductBalanceIfEnough(user.getId(), amount);
        if (updated == 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        // compute profit: ((amount * dailyReturnPercent)/100) * durationDays
        BigDecimal profit = amount.multiply(plan.totalReturnFactor());

        Investment inv = Investment.builder()
                .user(user)
                .planId(Integer.valueOf(plan.id))
                .planName(plan.name)
                .amount(amount)
                .asset(req.getAsset())
                .dailyReturnPercent(plan.dailyReturnPercent)
                .durationDays(plan.durationDays)
                .profit(profit)
                .status("active")
                .createdAt(OffsetDateTime.now())
                .build();

        Investment saved = investmentRepository.save(inv);

        return toDto(saved);
    }

    public List<InvestmentResponse> getHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Investment> list = investmentRepository.findByUserOrderByCreatedAtDesc(user);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    private InvestmentResponse toDto(Investment inv) {
        return InvestmentResponse.builder()
                .id(inv.getId())
                .createdAt(inv.getCreatedAt())
                .planId(inv.getPlanId())
                .planName(inv.getPlanName())
                .amount(inv.getAmount())
                .asset(inv.getAsset())
                .status(inv.getStatus())
                .profit(inv.getProfit())
                .dailyReturnPercent(inv.getDailyReturnPercent())
                .durationDays(inv.getDurationDays())
                .build();
    }
    public BigDecimal getTotalInvestment(User user) {
        BigDecimal totalAmount = investmentRepository.sumAmountByUser(user);
        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public BigDecimal getTotalProfit(User user) {
        BigDecimal totalProfit = investmentRepository.sumProfitByUser(user);
        return totalProfit != null ? totalProfit : BigDecimal.ZERO;
    }
}
