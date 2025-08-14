package com.investment.backend.repository;
import com.investment.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByReferralCode(String referralCode);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);
    @Modifying
    @Query("""
        UPDATE User u
        SET u.balance = u.balance - :amount
        WHERE u.id = :userId
          AND u.balance >= :amount
    """)
    int deductBalanceIfEnough(@Param("userId") Long userId,
                              @Param("amount") BigDecimal amount);
}