package com.payshield.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB Document - Failed Transactions for Retry
 */
@Document(collection = "failed_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "retry_idx", def = "{'status': 1, 'nextRetryAt': 1}")
public class FailedTransaction {

    @Id
    private String id;

    @Indexed(unique = true)
    private String transactionId;

    private Map<String, Object> originalPayload;

    @Indexed
    private FailureReason failureReason;

    private String errorDetails;

    @Indexed
    @Builder.Default
    private Integer retryCount = 0;

    @Builder.Default
    private Integer maxRetries = 5;

    private Instant nextRetryAt;

    private Instant lastRetryAt;

    @Indexed
    @Builder.Default
    private RetryStatus status = RetryStatus.PENDING_RETRY;

    @Indexed
    private Instant createdAt;

    /**
     * Calculate next retry time using exponential backoff
     * Formula: 2^retryCount * 60 seconds
     */
    public Instant calculateNextRetryTime() {
        int delaySeconds = (int) Math.pow(2, retryCount) * 60;
        return Instant.now().plusSeconds(delaySeconds);
    }

    public boolean canRetry() {
        return retryCount < maxRetries && status == RetryStatus.PENDING_RETRY;
    }

    public void incrementRetry() {
        this.retryCount++;
        this.lastRetryAt = Instant.now();
        if (this.retryCount >= this.maxRetries) {
            this.status = RetryStatus.EXHAUSTED;
        } else {
            this.nextRetryAt = calculateNextRetryTime();
        }
    }

    public enum FailureReason {
        DOWNSTREAM_ERROR,
        AI_UNCERTAINTY,
        TIMEOUT,
        VALIDATION_ERROR,
        SYSTEM_ERROR
    }

    public enum RetryStatus {
        PENDING_RETRY,
        RETRYING,
        EXHAUSTED,
        RESOLVED
    }
}
