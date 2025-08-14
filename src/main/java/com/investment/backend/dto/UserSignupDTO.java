package com.investment.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserSignupDTO {
    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
//    @Pattern(
//            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
//            message = "Password must contain at least one digit, one lowercase, one uppercase letter and one special character"
//    )
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    // Referral code (optional)
    private String referralCode;

}