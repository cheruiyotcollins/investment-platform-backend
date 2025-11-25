package com.investment.backend.repository;

import com.investment.backend.entity.WithdrawalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalRepository extends JpaRepository<WithdrawalRecord, Long> {
}
