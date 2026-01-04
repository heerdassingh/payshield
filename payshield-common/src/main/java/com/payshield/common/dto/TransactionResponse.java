package com.payshield.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transaction Response DTO - API response after publishing to Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private String transactionId;
    private String status;
    private String message;
    private BigDecimal amount;
    private String currency;
    private Instant submittedAt;

    public static TransactionResponse accepted(String transactionId, BigDecimal amount, String currency) {
        return TransactionResponse.builder()
                .transactionId(transactionId)
                .status("ACCEPTED")
                .message("Transaction submitted for processing")
                .amount(amount)
                .currency(currency)
                .submittedAt(Instant.now())
                .build();
    }

    public static TransactionResponse rejected(String message) {
        return TransactionResponse.builder()
                .status("REJECTED")
                .message(message)
                .submittedAt(Instant.now())
                .build();
    }
}
