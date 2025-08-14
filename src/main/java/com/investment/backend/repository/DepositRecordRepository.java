package com.investment.backend.repository;

import com.investment.backend.entity.DepositRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositRecordRepository extends JpaRepository<DepositRecord, Long>, JpaSpecificationExecutor<DepositRecord> {
    @Query("SELECT d FROM DepositRecord d WHERE d.userId = :userId")
    List<DepositRecord> findByUserId(@Param("userId") Long userId);

    @Query("SELECT d FROM DepositRecord d WHERE d.status = :status")
    List<DepositRecord> findByStatus(@Param("status") String status);

    @Query("SELECT d FROM DepositRecord d WHERE d.userId = :userId AND d.status = :status")
    List<DepositRecord> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") String status
    );
}