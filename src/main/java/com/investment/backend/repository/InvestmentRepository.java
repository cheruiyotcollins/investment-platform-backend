package com.investment.backend.repository;


import com.investment.backend.entity.Investment;
import com.investment.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserOrderByCreatedAtDesc(User user);

    List<Investment> findByStatus(String active);
}
