package com.payshield.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB Document - Fraud Alert Records
 */
@Document(collection = "fraud_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert {

    @Id
    private String id;

    @Indexed(unique = true)
    private String alertId;

    @Indexed
    private String transactionId;

    private String userId;

    @Indexed
    private AlertType alertType;

    @Indexed
    private Severity severity;

    private Double fraudScore;

    private Map<String, Object> details;

    @Builder.Default
    private Boolean acknowledged = false;

    private String acknowledgedBy;

    private Instant acknowledgedAt;

    @Indexed
    private Instant createdAt;

    public enum AlertType {
        FRAUD_DETECTED,
        RETRY_EXHAUSTED,
        USER_NOT_FOUND,
        HIGH_RISK_TRANSACTION,
        ANOMALY_DETECTED
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
