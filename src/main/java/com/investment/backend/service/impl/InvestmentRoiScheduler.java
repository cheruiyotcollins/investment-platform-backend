package com.investment.backend.service.impl;

import com.investment.backend.entity.Investment;
import com.investment.backend.entity.InvestmentPlan;
import com.investment.backend.repository.InvestmentRepository;
import com.investment.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InvestmentRoiScheduler {

    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;

    public InvestmentRoiScheduler(InvestmentRepository investmentRepository,UserRepository userRepository) {
        this.investmentRepository = investmentRepository;
        this.userRepository=userRepository;
    }

    /**
     * Runs at midnight every day to update ROI for active investments.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void calculateDailyROI() {
        log.info("Starting daily ROI calculation for investments...");

        List<Investment> activeInvestments = investmentRepository.findByStatus("active");
        LocalDateTime now = LocalDateTime.now();

        // Map to store total ROI per user
        Map<Long, BigDecimal> userTotalRoiMap = new HashMap<>();

        for (Investment inv : activeInvestments) {
            BigDecimal roi = calculateInvestmentROI(inv, now);
            inv.setProfit(roi.setScale(2, RoundingMode.HALF_UP));

            // If investment matured, mark as completed
            long daysElapsed = ChronoUnit.DAYS.between(inv.getCreatedAt().toLocalDateTime(), now);
            if (daysElapsed >= inv.getDurationDays()) {
                inv.setStatus("completed");
            }

            investmentRepository.save(inv);

            // Add ROI to user's total
            userTotalRoiMap.merge(
                    inv.getUser().getId(),
                    roi,
                    BigDecimal::add
            );

            log.debug("Updated ROI for investment {}: {}", inv.getId(), roi);
        }

        // Update each user's total ROI
        userTotalRoiMap.forEach((userId, totalRoi) -> {
            userRepository.findById(userId).ifPresent(user -> {
                user.setRoi(totalRoi.setScale(2, RoundingMode.HALF_UP));
                userRepository.save(user);
                log.debug("Updated total ROI for user {}: {}", user.getId(), totalRoi);
            });
        });

        log.info("Daily ROI calculation completed for {} active investments", activeInvestments.size());
    }

    /**
     * Calculates ROI for a single investment based on its plan percentage and duration.
     */
    public BigDecimal calculateInvestmentROI(Investment inv, LocalDateTime now) {
        long daysElapsed = ChronoUnit.DAYS.between(inv.getCreatedAt().toLocalDateTime(), now);
        if (daysElapsed < 0) daysElapsed = 0;

        // Cap days to the investment duration
        long effectiveDays = Math.min(daysElapsed, inv.getDurationDays());

        BigDecimal dailyRate = BigDecimal.valueOf(inv.getDailyReturnPercent()).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        return inv.getAmount()
                .multiply(dailyRate)
                .multiply(BigDecimal.valueOf(effectiveDays));
    }
}