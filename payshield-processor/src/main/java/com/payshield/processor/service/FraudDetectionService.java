package com.payshield.processor.service;

import com.payshield.common.dto.TransactionEvent;
import com.payshield.common.model.TransactionRecord;
import com.payshield.common.repository.TransactionRecordRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * AI Fraud Detection Service — calls Python AI microservice (Isolation Forest + RAG)
 * 
 * Features extracted in Java and sent to Python for ML prediction:
 * - Amount normalized (transaction amount / user average)
 * - Transaction frequency in last hour
 * - Transaction frequency in last 24 hours
 * - Time of day anomaly
 * - High amount flag
 * - Velocity detection
 * 
 * Falls back to rule-based scoring if Python AI service is unavailable.
 */
@Service
@Slf4j
public class FraudDetectionService {

    private final TransactionRecordRepository transactionRecordRepository;
    private final RestTemplate aiRestTemplate;
    private final Counter fraudDetectedCounter;
    private final Counter transactionsCheckedCounter;
    private final Counter aiServiceFallbackCounter;
    private final Timer fraudCheckTimer;

    @Value("${payshield.ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

    // Thresholds
    private static final double FRAUD_THRESHOLD = 0.7;
    private static final double HIGH_RISK_THRESHOLD = 0.5;
    private static final int MAX_TRANSACTIONS_PER_HOUR = 10;
    private static final int MAX_TRANSACTIONS_PER_DAY = 50;

    public FraudDetectionService(
            TransactionRecordRepository transactionRecordRepository,
            @Qualifier("aiRestTemplate") RestTemplate aiRestTemplate,
            MeterRegistry meterRegistry) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.aiRestTemplate = aiRestTemplate;

        this.fraudDetectedCounter = Counter.builder("fraud.detected")
                .description("Number of fraud cases detected")
                .register(meterRegistry);

        this.transactionsCheckedCounter = Counter.builder("fraud.checked")
                .description("Number of transactions checked for fraud")
                .register(meterRegistry);

        this.aiServiceFallbackCounter = Counter.builder("fraud.ai.fallback")
                .description("Number of times fallback scoring was used")
                .register(meterRegistry);

        this.fraudCheckTimer = Timer.builder("fraud.check.duration")
                .description("Time to perform fraud check")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        log.info("Fraud Detection Service initialized — Python AI endpoint: {}", aiServiceUrl);
        log.info("Fraud threshold: {}, High-risk threshold: {}", FRAUD_THRESHOLD, HIGH_RISK_THRESHOLD);
    }

    /**
     * Perform AI-based fraud detection on a transaction.
     * Extracts features in Java, sends to Python AI microservice for prediction.
     */
    public TransactionEvent detectFraud(TransactionEvent event) {
        return fraudCheckTimer.record(() -> {
            transactionsCheckedCounter.increment();

            // Extract features
            Map<String, Object> features = extractFeatures(event);
            Map<String, Double> featureScores = new HashMap<>();
            features.forEach((k, v) -> {
                if (v instanceof Number) {
                    featureScores.put(k, ((Number) v).doubleValue());
                }
            });
            event.setFraudFeatures(featureScores);

            // Call Python AI service
            double fraudScore;
            String fraudReason;
            try {
                Map<String, Object> prediction = callPythonAI(features);
                fraudScore = ((Number) prediction.getOrDefault("fraud_score", 0.0)).doubleValue();
                fraudReason = (String) prediction.getOrDefault("fraud_reason", "AI model prediction");
                
                // Add RAG explanation if available
                String ragExplanation = (String) prediction.get("rag_explanation");
                if (ragExplanation != null && !ragExplanation.isEmpty()) {
                    fraudReason = fraudReason + " | " + ragExplanation;
                }
                
                log.info("Python AI prediction: transactionId={}, score={}, reason={}",
                        event.getTransactionId(), fraudScore, prediction.get("fraud_reason"));
                        
            } catch (Exception e) {
                log.warn("Python AI service unavailable, using fallback scoring: {}", e.getMessage());
                aiServiceFallbackCounter.increment();
                fraudScore = calculateFallbackScore(featureScores);
                fraudReason = determineFallbackReason(featureScores, fraudScore);
            }

            event.setFraudScore(fraudScore);

            // Determine if fraudulent
            boolean isFraudulent = fraudScore >= FRAUD_THRESHOLD;
            event.setIsFraudulent(isFraudulent);

            if (isFraudulent) {
                fraudDetectedCounter.increment();
                event.setFraudReason(fraudReason);
                event.setEventType(TransactionEvent.EventType.FRAUD_DETECTED);
                event.setStage("FRAUD_DETECTED");
                log.warn("FRAUD DETECTED: transactionId={}, score={}, reason={}",
                        event.getTransactionId(), fraudScore, fraudReason);
            } else {
                event.setFraudReason(fraudReason);
                event.setEventType(TransactionEvent.EventType.FRAUD_CHECKED);
                event.setStage("FRAUD_CLEARED");
                log.info("Transaction cleared: transactionId={}, fraudScore={}",
                        event.getTransactionId(), fraudScore);
            }

            return event;
        });
    }

    /**
     * Call the Python AI microservice for fraud prediction
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callPythonAI(Map<String, Object> features) {
        String predictUrl = aiServiceUrl + "/api/v1/predict";
        
        Map<String, Object> response = aiRestTemplate.postForObject(
                predictUrl, features, Map.class);
        
        if (response == null) {
            throw new RuntimeException("Empty response from AI service");
        }
        
        return response;
    }

    /**
     * Extract features for ML model
     */
    private Map<String, Object> extractFeatures(TransactionEvent event) {
        Map<String, Object> features = new HashMap<>();
        String userId = event.getUserId();
        Instant now = Instant.now();

        // Transaction metadata for Python service
        features.put("transaction_id", event.getTransactionId());
        features.put("user_id", event.getUserId());
        features.put("merchant_id", event.getMerchantId());
        features.put("amount", event.getAmount().doubleValue());
        features.put("currency", event.getCurrency());

        // Feature 1: Amount normalized (0-1 scale)
        double amount = event.getAmount().doubleValue();
        double amountNormalized = Math.min(1.0, amount / 10000.0);
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
        double timeAnomaly = (hour >= 1 && hour <= 5) ? 0.8 : 0.0;
        features.put("time_anomaly", timeAnomaly);

        // Feature 5: High amount flag
        double highAmountFlag = amount > 5000 ? 0.6 : (amount > 1000 ? 0.3 : 0.0);
        features.put("high_amount_flag", highAmountFlag);

        // Feature 6: Velocity (rapid transactions)
        double velocity = frequencyHour > 0.5 ? 0.7 : 0.0;
        features.put("velocity", velocity);

        // Optional context
        if (event.getDeviceId() != null) features.put("device_id", event.getDeviceId());
        if (event.getIpAddress() != null) features.put("ip_address", event.getIpAddress());
        if (event.getLatitude() != null) features.put("latitude", event.getLatitude());
        if (event.getLongitude() != null) features.put("longitude", event.getLongitude());

        log.debug("Extracted features for transactionId={}: {}", event.getTransactionId(), features);
        return features;
    }

    /**
     * Fallback: calculate fraud score locally when Python AI is unavailable
     */
    private double calculateFallbackScore(Map<String, Double> features) {
        double score = 0.0;

        score += features.getOrDefault("amount_normalized", 0.0) * 0.25;
        score += features.getOrDefault("frequency_1h", 0.0) * 0.20;
        score += features.getOrDefault("frequency_24h", 0.0) * 0.15;
        score += features.getOrDefault("time_anomaly", 0.0) * 0.15;
        score += features.getOrDefault("high_amount_flag", 0.0) * 0.15;
        score += features.getOrDefault("velocity", 0.0) * 0.10;

        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Fallback: determine fraud reason locally
     */
    private String determineFallbackReason(Map<String, Double> features, double score) {
        StringBuilder reason = new StringBuilder("[Fallback] ");

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

        if (reason.length() <= 11) {
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
