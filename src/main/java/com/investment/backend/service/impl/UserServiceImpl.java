package com.investment.backend.service.impl;


import com.investment.backend.dto.WithdrawalRequestDTO;
import com.investment.backend.dto.WithdrawalResponseDTO;
import com.investment.backend.entity.Referral;
import com.investment.backend.entity.Transaction;
import com.investment.backend.entity.User;
import com.investment.backend.entity.WithdrawalRecord;
import com.investment.backend.exception.RegistrationException;
import com.investment.backend.repository.ReferralRepository;
import com.investment.backend.repository.TransactionRepository;
import com.investment.backend.repository.UserRepository;
import com.investment.backend.repository.WithdrawalRepository;
import com.investment.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final ReferralRepository referralRepository;
    private final WithdrawalRepository withdrawalRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                       TransactionRepository transactionRepository,ReferralRepository referralRepository, WithdrawalRepository withdrawalRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.referralRepository=referralRepository;
        this.withdrawalRepository= withdrawalRepository;;

    }

    @Override
    @Transactional
    public User registerUser(User user) {
        // Check if user already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RegistrationException("Email already in use");
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RegistrationException("Username already taken");
        }
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


    @Override
    @Transactional
    public void processReferral(User user) {
        if (user.getReferredBy() == null) {
            return;
        }

        User referrer = userRepository.findByReferralCode(user.getReferredBy())
                .orElseThrow(() -> new RuntimeException("Invalid referral code"));

        // Check if a referral already exists between these users
        boolean referralExists = referralRepository.existsByReferrerAndReferredPerson(referrer, user);

        if (referralExists) {
            return; // Skip if already rewarded
        }

        // Apply referral bonus only if it's the first deposit (no existing referral)
        BigDecimal referralBonus = user.getBalance().multiply(new BigDecimal("0.10"));
        referrer.setBalance(referrer.getBalance().add(referralBonus));

        Referral referral = new Referral(referrer, user, referralBonus);
        referralRepository.save(referral);
        userRepository.save(referrer);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    private String generateReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    @Transactional
    public void processDeposit(Long userId, BigDecimal amount, String currency) {
        // 1. Validate input
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        // 2. Get the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Create and save transaction record
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setType("DEPOSIT");
        transaction.setStatus("VERIFIED");
        transactionRepository.save(transaction);

        updateUserBalance(user, amount, currency);
        //todo
        processReferral(user);
    }

    private void updateUserBalance(User user, BigDecimal amount, String currency) {
        BigDecimal currentBalance = user.getBalance().add(amount);
        user.setBalance(currentBalance);
        userRepository.save(user);
    }
    @Override
    @Transactional
    public WithdrawalResponseDTO processWithdrawal(Long userId, WithdrawalRequestDTO withdrawalRequest) {
        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // DEBUG: Log current state
        System.out.println("DEBUG - Before withdrawal:");
        System.out.println("User ID: " + userId);
        System.out.println("Current Balance: " + user.getBalance());
        System.out.println("Withdrawal Amount: " + withdrawalRequest.getAmount());

        // Validate sufficient funds
        BigDecimal currentBalance = user.getBalance();
        BigDecimal withdrawalAmount = withdrawalRequest.getAmount();

        if (currentBalance == null) {
            throw new RuntimeException("User balance is not available");
        }

        if (currentBalance.compareTo(withdrawalAmount) < 0) {
            throw new RuntimeException("Insufficient balance. Current balance: " + currentBalance + ", Requested: " + withdrawalAmount);
        }

        // Validate minimum withdrawal amount
        BigDecimal minimumWithdrawal = getMinimumWithdrawal(withdrawalRequest.getCurrency());
        if (withdrawalAmount.compareTo(minimumWithdrawal) < 0) {
            throw new RuntimeException("Minimum withdrawal amount for " + withdrawalRequest.getCurrency() + " is " + minimumWithdrawal);
        }

        try {
            // Deduct balance from user
            BigDecimal newBalance = currentBalance.subtract(withdrawalAmount);
            user.setBalance(newBalance);

            // DEBUG: Log before save
            System.out.println("DEBUG - After deduction, before save:");
            System.out.println("New Balance: " + newBalance);

            User savedUser = userRepository.save(user);

            // DEBUG: Log after save
            System.out.println("DEBUG - After user save:");
            System.out.println("Saved User Balance: " + savedUser.getBalance());

            // Verify the save worked
            User verifiedUser = userRepository.findById(userId).orElse(null);
            System.out.println("DEBUG - Verified User Balance: " + (verifiedUser != null ? verifiedUser.getBalance() : "User not found"));

            // Create withdrawal record
            WithdrawalRecord withdrawal = new WithdrawalRecord();
            withdrawal.setUserId(userId);
            withdrawal.setAmount(withdrawalAmount);
            withdrawal.setCurrency(withdrawalRequest.getCurrency().toUpperCase());
            withdrawal.setWalletAddress(withdrawalRequest.getWalletAddress());
            withdrawal.setStatus("PENDING");
            withdrawal.setCreatedAt(LocalDateTime.now());

            // Save withdrawal record
            WithdrawalRecord savedWithdrawal = withdrawalRepository.save(withdrawal);

            // Log the withdrawal for admin review
            logWithdrawalForProcessing(savedWithdrawal);

            return new WithdrawalResponseDTO(
                    savedWithdrawal.getId(),
                    withdrawalAmount,
                    withdrawalRequest.getCurrency(),
                    withdrawalRequest.getWalletAddress(),
                    "PENDING",
                    LocalDateTime.now()
            );

        } catch (Exception e) {
            // Log the actual error
            System.err.println("ERROR in processWithdrawal: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Withdrawal processing failed. Please try again.");
        }
    }

    private BigDecimal getMinimumWithdrawal(String currency) {
        switch (currency.toUpperCase()) {
            case "BTC":
                return new BigDecimal("0.001"); // Minimum 0.001 BTC
            case "ETH":
                return new BigDecimal("0.01"); // Minimum 0.01 ETH
            case "USDT":
                return new BigDecimal("10"); // Minimum 10 USDT
            case "USDC":
                return new BigDecimal("10"); // Minimum 10 USDC
            default:
                return new BigDecimal("0.001"); // Default minimum
        }
    }

    private BigDecimal calculateNetworkFee(String currency, BigDecimal amount) {
        // This is a simplified fee calculation
        // In production, you'd want to fetch current network fees from an API
        switch (currency.toUpperCase()) {
            case "BTC":
                return new BigDecimal("0.0005"); // Fixed BTC network fee
            case "ETH":
                return new BigDecimal("0.005"); // Fixed ETH gas fee
            case "USDT":
            case "USDC":
                return new BigDecimal("1"); // Fixed stablecoin fee
            default:
                return new BigDecimal("0.001"); // Default fee
        }
    }

    private void logWithdrawalForProcessing(WithdrawalRecord withdrawal) {
        // In production, you might want to:
        // 1. Send to message queue for processing
        // 2. Notify admins
        // 3. Log to audit system
        System.out.println("Withdrawal initiated - ID: " + withdrawal.getId() +
                ", Amount: " + withdrawal.getAmount() +
                " " + withdrawal.getCurrency() +
                ", Wallet: " + withdrawal.getWalletAddress());
    }
}
