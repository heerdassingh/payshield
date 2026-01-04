package com.payshield.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MySQL Entity - Merchant Master Data
 */
@Entity
@Table(name = "merchants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "mcc_code", length = 10)
    private String mccCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MerchantStatus status = MerchantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "daily_volume_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyVolumeLimit = new BigDecimal("1000000.00");

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == MerchantStatus.ACTIVE;
    }

    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH;
    }

    public enum MerchantStatus {
        ACTIVE, INACTIVE, BLOCKED, UNDER_REVIEW
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
}
