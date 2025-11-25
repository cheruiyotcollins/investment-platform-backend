package com.investment.backend.service;

import com.investment.backend.entity.DepositRecord;
import com.investment.backend.repository.DepositRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class DepositService {

    private final DepositRecordRepository depositRecordRepository;

    @Autowired
    private UserService userService;

    @Autowired
    public DepositService(DepositRecordRepository depositRecordRepository) {
        this.depositRecordRepository = depositRecordRepository;
    }

    @Transactional
    public DepositRecord saveDepositRecord(DepositRecord record) {
        return depositRecordRepository.save(record);
    }

    @Transactional
    public DepositRecord updateDepositStatus(Long depositId, String status, String remarks) {
        DepositRecord record = depositRecordRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit record not found"));

        record.setStatus(status);
        record.setRemarks(remarks);
        record.setProcessedAt(LocalDateTime.now());

        return depositRecordRepository.save(record);
    }

    public Page<DepositRecord> findByFilters(Long userId, String status, LocalDate fromDate,
                                             LocalDate toDate, String currency, Pageable pageable) {
        Specification<DepositRecord> spec = Specification.allOf();

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (currency != null && !currency.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("currency"), currency));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(LocalTime.MAX)));
        }

        return depositRecordRepository.findAll(spec, pageable);
    }

    @Transactional
    public void approveDeposit(Long id, String remarks) {
        DepositRecord deposit = depositRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deposit record not found"));

        deposit.setStatus("APPROVED");
        deposit.setRemarks(remarks);
        deposit.setVerifiedAt(LocalDateTime.now());

        depositRecordRepository.save(deposit);
        userService.processDeposit(deposit.getUserId(), deposit.getAmount(), deposit.getCurrency());
    }

    @Transactional
    public void disapproveDeposit(Long id, String remarks) {
        DepositRecord deposit = depositRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deposit record not found"));

        deposit.setStatus("DISAPPROVED");
        deposit.setRemarks(remarks);
        deposit.setVerifiedAt(LocalDateTime.now());

        depositRecordRepository.save(deposit);
    }
}