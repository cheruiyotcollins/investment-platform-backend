package com.investment.backend.service;

import com.investment.backend.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


public interface UserService {
    User registerUser(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    User getUserById(Long id);
    void processReferral(User user);
    List<User> findAll();
    User saveUser(User user);
    public void processDeposit(Long userId, BigDecimal amount, String currency);
}