package com.investment.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReferralDTO {
    private String referralCode;
    private List<ReferredUsers> referredUsers;
}
