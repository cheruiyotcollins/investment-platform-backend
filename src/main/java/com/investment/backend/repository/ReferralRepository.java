package com.investment.backend.repository;

import com.investment.backend.entity.Referral;
import com.investment.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    boolean existsByReferrerAndReferredPerson(User referrer, User referee);
    List<Referral> findByReferrer(User referrer);
}
