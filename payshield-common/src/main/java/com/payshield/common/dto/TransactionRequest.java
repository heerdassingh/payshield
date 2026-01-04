package com.payshield.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Transaction Request DTO - Incoming API request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private String type = "PAYMENT";

    private String description;

    private String referenceId;

    // Additional context for fraud detection
    private String deviceId;
    private String ipAddress;
    private String userAgent;
    private Double latitude;
    private Double longitude;
}
