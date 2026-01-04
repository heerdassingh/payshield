package com.payshield.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Kafka Transaction Event - Internal message format for Kafka pipeline
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    private String transactionId;
    private String userId;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String type;
    private String description;
    private String referenceId;

    // Context for fraud detection
    private String deviceId;
    private String ipAddress;
    private String userAgent;
    private Double latitude;
    private Double longitude;

    // Processing metadata
    private EventType eventType;
    private String stage;
    private Instant createdAt;
    private Instant processedAt;

    // Validation results
    private Boolean userValid;
    private Boolean merchantValid;
    private String validationError;

    // Fraud detection results
    private Double fraudScore;
    private Boolean isFraudulent;
    private String fraudReason;
    private Map<String, Double> fraudFeatures;

    public enum EventType {
        INCOMING, // Just received from API
        USER_VALIDATED, // User validation passed
        USER_NOT_FOUND, // User validation failed
        FRAUD_CHECKED, // Fraud detection completed
        FRAUD_DETECTED, // Fraud was detected
        PROCESSING, // Being processed
        COMPLETED, // Successfully completed
        FAILED // Processing failed
    }

    public static TransactionEvent fromRequest(TransactionRequest request, String transactionId) {
        return TransactionEvent.builder()
                .transactionId(transactionId)
                .userId(request.getUserId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .type(request.getType())
                .description(request.getDescription())
                .referenceId(request.getReferenceId())
                .deviceId(request.getDeviceId())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .eventType(EventType.INCOMING)
                .stage("SUBMITTED")
                .createdAt(Instant.now())
                .build();
    }
}
