package com.investment.backend.service;

import com.investment.backend.config.FileStorageProperties;
import com.investment.backend.entity.DepositRecord;
import com.investment.backend.repository.DepositRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
public class DepositService {

    private final DepositRecordRepository depositRecordRepository;
    @Autowired
    private UserService userService;

    private final Path fileStorageLocation;

    @Autowired
    public DepositService(DepositRecordRepository depositRecordRepository,
                          FileStorageProperties fileStorageProperties) {
        this.depositRecordRepository = depositRecordRepository;

        // Configure the file storage location
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }
    public String storeFile(MultipartFile file) throws IOException {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) :
                "";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Resolve path and copy file
        Path targetLocation = this.fileStorageLocation.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFilename;
    }

    @Transactional
    public DepositRecord storeFileForDeposit(Long depositRecordId, MultipartFile file) throws IOException {
        DepositRecord record = depositRecordRepository.findById(depositRecordId)
                .orElseThrow(() -> new RuntimeException("Deposit record not found"));

        String filename = storeFile(file);
        record.setScreenshotPath(filename);

        return depositRecordRepository.save(record);
    }
    public Path loadFile(String filename) {
        return fileStorageLocation.resolve(filename).normalize();
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
        Specification<DepositRecord> spec = Specification.allOf(); // replaces where(null)

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
                .orElseThrow();

        deposit.setStatus("APPROVED");
        deposit.setRemarks(remarks);
        deposit.setVerifiedAt(LocalDateTime.now());

        depositRecordRepository.save(deposit);
        userService.processDeposit(deposit.getUserId(), deposit.getAmount(), deposit.getCurrency());
    }

    @Transactional
    public void disapproveDeposit(Long id, String remarks) {
        DepositRecord deposit = depositRecordRepository.findById(id)
                .orElseThrow();

        deposit.setStatus("DISAPPROVED");
        deposit.setRemarks(remarks);
        deposit.setVerifiedAt(LocalDateTime.now());

        depositRecordRepository.save(deposit);
    }
}
