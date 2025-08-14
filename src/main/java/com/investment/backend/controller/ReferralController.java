package com.investment.backend.controller;

import com.investment.backend.dto.ReferralDTO;
import com.investment.backend.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    /**
     * Fetches referral data for a user by their ID
     * @param userId The ID of the referring user
     * @return ReferralDTO containing referral code and referred users
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ReferralDTO> getUserReferrals(@PathVariable Long userId) {
        ReferralDTO referralData = referralService.fetchUserReferrals(userId);
        return ResponseEntity.ok(referralData);
    }

    /**
     * Alternative: Fetch by referral code instead of user ID
     */
//    @GetMapping("/code/{referralCode}")
//    public ResponseEntity<ReferralDTO> getReferralsByCode(@PathVariable String referralCode) {
//        // You would need to add this method to your service
//        ReferralDTO referralData = referralService.fetchReferralsByCode(referralCode);
//        return ResponseEntity.ok(referralData);
//    }
}