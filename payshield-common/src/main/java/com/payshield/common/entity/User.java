package com.payshield.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MySQL Entity - Master User Data for Validation
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "max_transaction_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal maxTransactionLimit = new BigDecimal("10000.00");

    @Column(name = "daily_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyLimit = new BigDecimal("50000.00");

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyLimit = new BigDecimal("500000.00");

    @Column(name = "risk_score", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal riskScore = BigDecimal.ZERO;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

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
        return status == UserStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return status == UserStatus.BLOCKED;
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, BLOCKED, PENDING
    }

    public enum UserRole {
        USER, MERCHANT, ADMIN
    }
}
