package com.investment.backend.service;

import com.investment.backend.dto.ReferralDTO;
import com.investment.backend.dto.ReferredUsers;
import com.investment.backend.entity.Referral;
import com.investment.backend.entity.User;
import com.investment.backend.repository.ReferralRepository;
import com.investment.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReferralService {

    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;

    public ReferralService(UserRepository userRepository, ReferralRepository referralRepository) {
        this.userRepository = userRepository;
        this.referralRepository = referralRepository;
    }

    /**
     * Fetches referral data for a given user, including their referred users and rewards.
     * @param userId The ID of the referring user.
     * @return ReferralDTO containing referral code and list of referred users.
     */
    @Transactional(readOnly = true)
    public ReferralDTO fetchUserReferrals(Long userId) {
        User referrer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch all referrals where this user is the referrer
        List<Referral> referrals = referralRepository.findByReferrer(referrer);

        // Map referrals to ReferredUsers DTO
        List<ReferredUsers> referredUsers = referrals.stream()
                .map(this::mapToReferredUser)
                .collect(Collectors.toList());

        // Build and return the DTO
        return ReferralDTO.builder()
                .referralCode(referrer.getReferralCode())
                .referredUsers(referredUsers)
                .build();
    }

    /**
     * Maps a Referral entity to a ReferredUsers DTO.
     */
    private ReferredUsers mapToReferredUser(Referral referral) {
        User referee = referral.getReferredPerson();

        return ReferredUsers.builder()
                .name(referee.getUsername()) // Assuming User has getName()
                .status(referral.isPaid() ? "PAID" : "PENDING") // Customize based on your logic
                .date(referral.getReferralDate())
                .reward(referral.getAmount())
                .build();
    }
}