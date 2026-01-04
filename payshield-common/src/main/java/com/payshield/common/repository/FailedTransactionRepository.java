package com.payshield.common.repository;

import com.payshield.common.model.FailedTransaction;
import com.payshield.common.model.FailedTransaction.RetryStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository - Failed Transactions for Retry
 */
@Repository
public interface FailedTransactionRepository extends MongoRepository<FailedTransaction, String> {

    Optional<FailedTransaction> findByTransactionId(String transactionId);

    @Query("{ 'status': 'PENDING_RETRY', 'nextRetryAt': { $lte: ?0 }, 'retryCount': { $lt: ?1 } }")
    List<FailedTransaction> findTransactionsToRetry(Instant now, int maxRetries);

    List<FailedTransaction> findByStatusAndNextRetryAtBeforeAndRetryCountLessThan(
            RetryStatus status, Instant before, int maxRetries);

    List<FailedTransaction> findByStatus(RetryStatus status);

    long countByStatus(RetryStatus status);

    boolean existsByTransactionId(String transactionId);
}
