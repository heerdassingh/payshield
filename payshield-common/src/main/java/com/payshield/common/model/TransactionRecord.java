package com.payshield.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * MongoDB Document - Successful Transaction Records
 */
@Document(collection = "user_data_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord {

    @Id
    private String id;

    @Indexed(unique = true)
    private String transactionId;

    @Indexed
    private String userId;

    private String merchantId;

    private BigDecimal amount;

    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private TransactionType type = TransactionType.PAYMENT;

    @Indexed
    @Builder.Default
    private TransactionStatus status = TransactionStatus.COMPLETED;

    private Double fraudScore;

    private String fraudReason;

    @Indexed
    private Instant processedAt;

    private Map<String, Object> metadata;

    public enum TransactionType {
        PAYMENT, TRANSFER, WITHDRAWAL, DEPOSIT
    }

    public enum TransactionStatus {
        COMPLETED, PENDING, REJECTED
    }
}
