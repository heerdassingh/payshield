package com.payshield.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Transaction Status DTO - For checking transaction status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatus {

    private String transactionId;
    private String status;
    private String stage;
    private Double fraudScore;
    private String fraudReason;
    private Boolean isFraudulent;
    private Integer retryCount;
    private Instant submittedAt;
    private Instant processedAt;
    private String message;

    public enum Stage {
        SUBMITTED,
        VALIDATING_USER,
        USER_VALIDATED,
        USER_NOT_FOUND,
        FRAUD_CHECKING,
        FRAUD_DETECTED,
        PROCESSING,
        COMPLETED,
        FAILED,
        RETRYING
    }
}
