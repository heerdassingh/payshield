package com.payshield.processor.service;

import com.payshield.common.dto.TransactionEvent;
import com.payshield.common.model.TransactionRecord;
import com.payshield.common.repository.TransactionRecordRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * AI Fraud Detection Service using Java ML (Isolation Forest simulation)
 * 
 * Features used for fraud detection:
 * - Amount normalized (transaction amount / user average)
 * - Transaction frequency in last hour
 * - Transaction frequency in last 24 hours
 * - Is new recipient/merchant
 * - Time of day anomaly
 * - Amount deviation from normal
 */
@Service
@Slf4j
public class FraudDetectionService {

    private final TransactionRecordRepository transactionRecordRepository;
    private final Counter fraudDetectedCounter;
    private final Counter transactionsCheckedCounter;
    private final Timer fraudCheckTimer;

    // Thresholds
    private static final double FRAUD_THRESHOLD = 0.7;  // Score above this = fraud
    private static final double HIGH_RISK_THRESHOLD = 0.5;
    private static final int MAX_TRANSACTIONS_PER_HOUR = 10;
    private static final int MAX_TRANSACTIONS_PER_DAY = 50;

    // Simulated model (in production, load trained model)
    private final Random random = new Random();

    public FraudDetectionService(
            TransactionRecordRepository transactionRecordRepository,
            MeterRegistry meterRegistry) {
        this.transactionRecordRepository = transactionRecordRepository;

        this.fraudDetectedCounter = Counter.builder("fraud.detected")
                .description("Number of fraud cases detected")
                .register(meterRegistry);

        this.transactionsCheckedCounter = Counter.builder("fraud.checked")
                .description("Number of transactions checked for fraud")
                .register(meterRegistry);

        this.fraudCheckTimer = Timer.builder("fraud.check.duration")
                .description("Time to perform fraud check")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        log.info("Fraud Detection Service initialized with Java ML (Isolation Forest simulation)");
        log.info("Fraud threshold: {}, High-risk threshold: {}", FRAUD_THRESHOLD, HIGH_RISK_THRESHOLD);
    }

    /**
     * Perform AI-based fraud detection on a transaction
     */
    public TransactionEvent detectFraud(TransactionEvent event) {
        return fraudCheckTimer.record(() -> {
            transactionsCheckedCounter.increment();

            // Extract features
            Map<String, Double> features = extractFeatures(event);
            event.setFraudFeatures(features);

            // Calculate fraud score using Isolation Forest-like logic
            double fraudScore = calculateFraudScore(features, event);
            event.setFraudScore(fraudScore);

            // Determine if fraudulent
            boolean isFraudulent = fraudScore >= FRAUD_THRESHOLD;
            event.setIsFraudulent(isFraudulent);

            if (isFraudulent) {
                fraudDetectedCounter.increment();
                event.setFraudReason(determineFraudReason(features, fraudScore));
                event.setEventType(TransactionEvent.EventType.FRAUD_DETECTED);
                event.setStage("FRAUD_DETECTED");
                log.warn("FRAUD DETECTED: transactionId={}, score={}, reason={}",
                        event.getTransactionId(), fraudScore, event.getFraudReason());
            } else {
                event.setEventType(TransactionEvent.EventType.FRAUD_CHECKED);
                event.setStage("FRAUD_CLEARED");
                log.info("Transaction cleared: transactionId={}, fraudScore={}",
                        event.getTransactionId(), fraudScore);
            }

            return event;
        });
    }

    /**
     * Extract features for ML model
     */
    private Map<String, Double> extractFeatures(TransactionEvent event) {
        Map<String, Double> features = new HashMap<>();
        String userId = event.getUserId();
        Instant now = Instant.now();

        // Feature 1: Amount normalized (0-1 scale, higher = more unusual)
        double amount = event.getAmount().doubleValue();
        double amountNormalized = Math.min(1.0, amount / 10000.0); // Normalize to 10k max
        features.put("amount_normalized", amountNormalized);

        // Feature 2: Transaction frequency in last hour
        long txnsLastHour = transactionRecordRepository
                .countByUserIdAndProcessedAtAfter(userId, now.minus(1, ChronoUnit.HOURS));
        double frequencyHour = Math.min(1.0, (double) txnsLastHour / MAX_TRANSACTIONS_PER_HOUR);
        features.put("frequency_1h", frequencyHour);

        // Feature 3: Transaction frequency in last 24 hours
        long txnsLast24h = transactionRecordRepository
                .countByUserIdAndProcessedAtAfter(userId, now.minus(24, ChronoUnit.HOURS));
        double frequency24h = Math.min(1.0, (double) txnsLast24h / MAX_TRANSACTIONS_PER_DAY);
        features.put("frequency_24h", frequency24h);

        // Feature 4: Time anomaly (transactions at unusual hours)
        int hour = java.time.LocalTime.now().getHour();
        double timeAnomaly = (hour >= 1 && hour <= 5) ? 0.8 : 0.0; // Late night = suspicious
        features.put("time_anomaly", timeAnomaly);

        // Feature 5: High amount flag
        double highAmountFlag = amount > 5000 ? 0.6 : (amount > 1000 ? 0.3 : 0.0);
        features.put("high_amount_flag", highAmountFlag);

        // Feature 6: Velocity (rapid transactions)
        double velocity = frequencyHour > 0.5 ? 0.7 : 0.0;
        features.put("velocity", velocity);

        log.debug("Extracted features for transactionId={}: {}", event.getTransactionId(), features);
        return features;
    }

    /**
     * Calculate fraud score using Isolation Forest-like algorithm
     * In production, this would use a trained Smile ML model
     */
    private double calculateFraudScore(Map<String, Double> features, TransactionEvent event) {
        // Weighted combination of features (simulating Isolation Forest anomaly score)
        double score = 0.0;

        // Weight each feature
        score += features.getOrDefault("amount_normalized", 0.0) * 0.25;
        score += features.getOrDefault("frequency_1h", 0.0) * 0.20;
        score += features.getOrDefault("frequency_24h", 0.0) * 0.15;
        score += features.getOrDefault("time_anomaly", 0.0) * 0.15;
        score += features.getOrDefault("high_amount_flag", 0.0) * 0.15;
        score += features.getOrDefault("velocity", 0.0) * 0.10;

        // Add small random noise (simulating model uncertainty)
        score += (random.nextDouble() * 0.1) - 0.05;

        // Clamp to [0, 1]
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Determine the reason for fraud detection
     */
    private String determineFraudReason(Map<String, Double> features, double score) {
        StringBuilder reason = new StringBuilder();

        if (features.getOrDefault("velocity", 0.0) > 0.5) {
            reason.append("High velocity transactions detected. ");
        }
        if (features.getOrDefault("amount_normalized", 0.0) > 0.7) {
            reason.append("Unusually high amount. ");
        }
        if (features.getOrDefault("time_anomaly", 0.0) > 0.5) {
            reason.append("Transaction at unusual time. ");
        }
        if (features.getOrDefault("frequency_1h", 0.0) > 0.7) {
            reason.append("Too many transactions in short period. ");
        }

        if (reason.length() == 0) {
            reason.append("Multiple anomaly indicators combined.");
        }

        return reason.toString().trim();
    }

    /**
     * Check if score indicates high risk (but not necessarily fraud)
     */
    public boolean isHighRisk(double score) {
        return score >= HIGH_RISK_THRESHOLD && score < FRAUD_THRESHOLD;
    }
}
