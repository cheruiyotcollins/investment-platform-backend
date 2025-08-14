package com.investment.backend.service.impl;


import com.investment.backend.entity.Referral;
import com.investment.backend.entity.Transaction;
import com.investment.backend.entity.User;
import com.investment.backend.exception.RegistrationException;
import com.investment.backend.repository.ReferralRepository;
import com.investment.backend.repository.TransactionRepository;
import com.investment.backend.repository.UserRepository;
import com.investment.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final ReferralRepository referralRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                       TransactionRepository transactionRepository,ReferralRepository referralRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.referralRepository=referralRepository;

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
}
