package com.investment.backend.dto;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private double balance;
    private double roi;
    private LocalDateTime joinedAt;
    private String referralCode;
    // Exclude password and other sensitive fields
}