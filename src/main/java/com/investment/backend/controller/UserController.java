package com.investment.backend.controller;

import com.investment.backend.config.JwtTokenUtil;
import com.investment.backend.dto.*;
import com.investment.backend.entity.DepositRecord;
import com.investment.backend.entity.User;
import com.investment.backend.service.CustomUserDetailsService;
import com.investment.backend.service.DepositService;
import com.investment.backend.service.InvestmentService;
import com.investment.backend.service.impl.InvestmentRoiScheduler;
import com.investment.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private InvestmentRoiScheduler investmentRoiScheduler;
    @Autowired
    private  InvestmentService service;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Autowired
    DepositService depositService;
    private final CustomUserDetailsService customUserDetailsService;

    public UserController(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> signup(@Valid @RequestBody UserSignupDTO userSignupDTO) {
        // Validate password match
        if (!userSignupDTO.getPassword().equals(userSignupDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Convert DTO to entity
        User user = new User();
        user.setUsername(userSignupDTO.getUsername());
        user.setEmail(userSignupDTO.getEmail());
        user.setPassword(userSignupDTO.getPassword());
        user.setReferredBy(userSignupDTO.getReferralCode());
        // Generate and set referral code
        String referralCode = generateReferralCode(userSignupDTO.getUsername());
        user.setReferralCode(referralCode);
        user.setRole("user");

        // Save user
        User savedUser = userService.registerUser(user);

        // Convert to response DTO
        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(savedUser.getId());
        responseDTO.setUsername(savedUser.getUsername());
        responseDTO.setEmail(savedUser.getEmail());
        responseDTO.setJoinedAt(savedUser.getJoinedAt());
        responseDTO.setReferralCode(savedUser.getReferralCode()); // Include referral code in response
        // Set other fields as needed

        return ResponseEntity.ok(responseDTO);
    }
    private String generateReferralCode(String username) {
        // Generate a unique referral code combining username and random numbers
        String baseCode = username.substring(0, Math.min(username.length(), 4)).toUpperCase();
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 9999);
        return baseCode + randomNum;
    }

    @GetMapping("/{userId}/dashboard")
    public ResponseEntity<?> getUserDashboard(@PathVariable Long userId,
                                              Authentication authentication) {
        // Verify the authenticated user matches the requested userId
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User authenticatedUser = userService.findByUsername(userDetails.getUsername()).get();

        if (!authenticatedUser.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userService.getUserById(userId);

        // Fetch the total investment and profit for the user
        BigDecimal totalInvestment = service.getTotalInvestment(user);
        BigDecimal totalProfit = service.getTotalProfit(user);

        // Calculate the total value (principal + profit)
        BigDecimal totalValue = totalInvestment.add(totalProfit);
        totalValue.add(user.getBalance());

        return ResponseEntity.ok(Map.of(
                "user", user,
                "totalInvestment", totalInvestment,
                "totalProfit", totalProfit,
                "totalValue", totalValue
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            // 1. Load user details by identifier (username or email)
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginRequest.getEmail());
            System.out.println("Login Identifier: " + loginRequest.getEmail());
            System.out.println("Raw Password: " + loginRequest.getPassword());
            System.out.println("DB Password: " + userDetails.getPassword());

            // 2. Verify password (direct comparison since no hashing)
            if (!loginRequest.getPassword().equals(userDetails.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
            }

            // 3. Generate JWT token
            String token = jwtTokenUtil.generateToken(userDetails);

            // 4. Get full user details
            User user = userService.findByUsername(loginRequest.getEmail())
                    .or(() -> userService.findByEmail(loginRequest.getEmail()))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 5. Return response
            return ResponseEntity.ok(new AuthResponse(token, user));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }
    }


    @GetMapping("/auth/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtTokenUtil.getUsernameFromToken(token);
            User user = userService.findByUsername(username).get();

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    @PostMapping("/deposit/{userId}")
    public ResponseEntity<?> depositFunds(
            @PathVariable Long userId,
            @RequestParam("amount") String amount,
            @RequestParam("currency") String currency,
            @RequestHeader("Authorization") String authHeader) {

        // Verify token and user
        String token = authHeader.substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        User user = userService.getUserById(userId);
        if (!user.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Parse and validate amount
        BigDecimal depositAmount;
        try {
            depositAmount = new BigDecimal(amount);
            if (depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Amount must be positive");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid amount format");
        }

        try {
            // Create and save deposit record
            DepositRecord record = new DepositRecord();
            record.setUserId(userId);
            record.setAmount(depositAmount);
            record.setCurrency(currency);
            record.setStatus("PENDING_VERIFICATION");
            record.setCreatedAt(LocalDateTime.now());

            // Add logging to see what's being saved
            System.out.println("Saving deposit record: " + record.toString());

            DepositRecord savedRecord = depositService.saveDepositRecord(record);

            return ResponseEntity.ok(Map.of(
                    "message", "Deposit received",
                    "status", "pending_verification",
                    "requiresManualReview", true,
                    "depositId", savedRecord.getId()
            ));
        } catch (Exception e) {
            // Log the actual exception for debugging
            e.printStackTrace(); // This will show the actual error in logs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Deposit processing failed. Please try again.");
        }
    }



//    @GetMapping("/{userId}/wallets")
//    public ResponseEntity<?> getDepositWallets(@PathVariable Long userId) {
//        // In a real app, these would be your platform's wallet addresses
//        return ResponseEntity.ok(Map.of(
//                "btcAddress", "bc1qz4puhlmr8ewsj7h4gwdgzdafdznd299ky8j0mf",
//                "ethAddress", "0x1ff1d59A88E06d527148c53476DB72c0b3378cd6",
//                "usdtAddress", "0x1ff1d59A88E06d527148c53476DB72c0b3378cd6" // USDT contract address
//        ));
//    }
//@GetMapping("/{userId}/wallets")
//public ResponseEntity<?> getDepositWallets(@PathVariable Long userId) {
//    // In a real app, these would be your platform's wallet addresses
//    return ResponseEntity.ok(Map.of(
//            "btcAddress", "bc1qz2mkjqh6sg9pewjpsln0sxmxyg9xa4agekldzm",
//            "ethAddress", "0x8d2ecc15a247790b2ef99a5d940484c8cdfc4766",
//            "usdtAddress", "00x8d2ecc15a247790b2ef99a5d940484c8cdfc4766"
//    ));
//}

    @GetMapping("/{userId}/wallets")
    public ResponseEntity<?> getDepositWallets(@PathVariable Long userId) {
        // In a real app, these would be your platform's wallet addresses
        return ResponseEntity.ok(Map.of(
                "btcAddress", "38n5Y6cepPWDrJxL66QTJ8H9SAQnRvTbRW",
                "ethAddress", "0xF8d75E54d0BdE7F7B836927e0Ff721f4F6b9DC66",
                "usdtAddress", "TDSLa1vsJ5pPZyCnGajG2LGHJ4a3pRfvC7",
                "usdcAddress","0xF8d75E54d0BdE7F7B836927e0Ff721f4F6b9DC66"
        ));
    }

    private boolean validateWalletAddress(String currency, String address) {
        switch (currency) {
            case "BTC":
                return address.matches("^(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}$");
            case "ETH":
                return address.matches("^0x[a-fA-F0-9]{40}$");
            case "USDT":
                // USDT can be on multiple chains, we'll accept both ETH and TRON formats
                return address.matches("^0x[a-fA-F0-9]{40}$") ||
                        address.matches("^T[a-zA-HJ-NP-Z0-9]{33}$");
            default:
                return false;
        }
    }
    @GetMapping("/deposit/{userId}")
    public ResponseEntity<?> getUserDeposits(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String currency,
            @RequestHeader("Authorization") String authHeader) {

        // Verify token matches user
        String token = authHeader.substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        User user = userService.getUserById(userId);
        if (!user.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<DepositRecord> deposits = depositService.findByFilters(
                userId, status, fromDate, toDate, currency,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return ResponseEntity.ok(deposits);
    }

    @GetMapping("/deposit/all")
    public ResponseEntity<?> getAllDeposits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String currency) {

        Page<DepositRecord> deposits = depositService.findByFilters(
                null, status, fromDate, toDate, currency,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return ResponseEntity.ok(deposits);
    }
    @PutMapping("/deposit/{id}/approve")
    public ResponseEntity<?> approveDeposit(
            @PathVariable Long id,
            @RequestBody(required = false) RemarksRequest remarksRequest
    ) {
        depositService.approveDeposit(id, remarksRequest != null ? remarksRequest.getRemarks() : null);
        return ResponseEntity.ok(Map.of("message", "Deposit approved successfully"));
    }

    @PutMapping("/deposit/{id}/disapprove")
    public ResponseEntity<?> disapproveDeposit(
            @PathVariable Long id,
            @RequestBody(required = false) RemarksRequest remarksRequest
    ) {
        depositService.disapproveDeposit(id, remarksRequest != null ? remarksRequest.getRemarks() : null);
        return ResponseEntity.ok(Map.of("message", "Deposit disapproved successfully"));
    }
    @PostMapping("/{userId}/withdraw")
    public ResponseEntity<?> withdrawFunds(
            @PathVariable Long userId,
            @Valid @RequestBody WithdrawalRequestDTO withdrawalRequest,
            @RequestHeader("Authorization") String authHeader) {

        // Verify token and user
        String token = authHeader.substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        User user = userService.getUserById(userId);
        if (!user.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Validate wallet address
        if (!validateWalletAddress(withdrawalRequest.getCurrency(), withdrawalRequest.getWalletAddress())) {
            return ResponseEntity.badRequest().body("Invalid wallet address for the specified currency");
        }

        // Validate amount
        if (withdrawalRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Amount must be positive");
        }

        try {
            WithdrawalResponseDTO response = userService.processWithdrawal(userId, withdrawalRequest);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Withdrawal processing failed: " + e.getMessage());
        }
    }

}