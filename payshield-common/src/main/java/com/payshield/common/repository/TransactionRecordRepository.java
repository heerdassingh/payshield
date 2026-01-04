package com.payshield.common.repository;

import com.payshield.common.model.TransactionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository - Successful Transaction Records
 */
@Repository
public interface TransactionRecordRepository extends MongoRepository<TransactionRecord, String> {

    Optional<TransactionRecord> findByTransactionId(String transactionId);

    List<TransactionRecord> findByUserId(String userId);

    List<TransactionRecord> findByUserIdOrderByProcessedAtDesc(String userId);

    List<TransactionRecord> findByUserIdAndProcessedAtBetween(String userId, Instant start, Instant end);

    long countByUserIdAndProcessedAtAfter(String userId, Instant after);

    boolean existsByTransactionId(String transactionId);
}
