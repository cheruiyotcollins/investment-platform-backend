package com.investment.backend.repository;


import com.investment.backend.entity.Investment;
import com.investment.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserOrderByCreatedAtDesc(User user);

    List<Investment> findByStatus(String active);
    @Query("SELECT SUM(i.amount) FROM Investment i WHERE i.user = :user")
    BigDecimal sumAmountByUser(@Param("user") User user);

    @Query("SELECT SUM(i.profit) FROM Investment i WHERE i.user = :user")
    BigDecimal sumProfitByUser(@Param("user") User user);
}
