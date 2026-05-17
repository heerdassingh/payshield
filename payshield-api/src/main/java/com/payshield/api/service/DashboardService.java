package com.payshield.api.service;

import com.payshield.common.model.FraudAlert;
import com.payshield.common.model.TransactionRecord;
import com.payshield.common.repository.FraudAlertRepository;
import com.payshield.common.repository.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard Service - Aggregates data for the React frontend dashboard
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRecordRepository transactionRecordRepository;
    private final FraudAlertRepository fraudAlertRepository;

    @Value("${payshield.ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

    /**
     * Get overall dashboard statistics
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        Instant now = Instant.now();
        Instant last24h = now.minus(24, ChronoUnit.HOURS);
        Instant last7d = now.minus(7, ChronoUnit.DAYS);

        // Total transactions
        long totalTransactions = transactionRecordRepository.count();
        stats.put("totalTransactions", totalTransactions);

        // Transactions today
        long transactionsToday = transactionRecordRepository.count(); // simplified
        stats.put("transactionsToday", transactionsToday);

        // Active alerts
        long activeAlerts = fraudAlertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc().size();
        stats.put("activeAlerts", activeAlerts);

        // Fraud alerts by severity
        long criticalAlerts = fraudAlertRepository.countBySeverityAndAcknowledgedFalse(FraudAlert.Severity.CRITICAL);
        long highAlerts = fraudAlertRepository.countBySeverityAndAcknowledgedFalse(FraudAlert.Severity.HIGH);
        long mediumAlerts = fraudAlertRepository.countBySeverityAndAcknowledgedFalse(FraudAlert.Severity.MEDIUM);
        stats.put("criticalAlerts", criticalAlerts);
        stats.put("highAlerts", highAlerts);
        stats.put("mediumAlerts", mediumAlerts);

        // Recent alerts count (last 24h)
        long recentAlerts = fraudAlertRepository.countByCreatedAtAfter(last24h);
        stats.put("recentAlerts24h", recentAlerts);

        // Fraud rate estimation
        List<TransactionRecord> allRecords = transactionRecordRepository.findAll();
        long fraudCount = allRecords.stream()
                .filter(r -> r.getStatus() == TransactionRecord.TransactionStatus.REJECTED)
                .count();
        double fraudRate = totalTransactions > 0 ? (double) fraudCount / totalTransactions * 100 : 0;
        stats.put("fraudRate", Math.round(fraudRate * 100.0) / 100.0);
        stats.put("fraudCount", fraudCount);

        // Average fraud score
        double avgFraudScore = allRecords.stream()
                .filter(r -> r.getFraudScore() != null)
                .mapToDouble(TransactionRecord::getFraudScore)
                .average()
                .orElse(0.0);
        stats.put("avgFraudScore", Math.round(avgFraudScore * 10000.0) / 10000.0);

        return stats;
    }

    /**
     * Get recent transactions with fraud details
     */
    public List<Map<String, Object>> getRecentTransactions(int limit) {
        List<TransactionRecord> records = transactionRecordRepository.findAll();

        // Sort by processedAt descending and limit
        return records.stream()
                .sorted(Comparator.comparing(
                        TransactionRecord::getProcessedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get fraud trends over time (grouped by hour for the last 24h)
     */
    public List<Map<String, Object>> getFraudTrends() {
        Instant now = Instant.now();
        List<Map<String, Object>> trends = new ArrayList<>();

        // Get last 24 hours of data, grouped by hour
        for (int i = 23; i >= 0; i--) {
            Instant hourStart = now.minus(i + 1, ChronoUnit.HOURS);
            Instant hourEnd = now.minus(i, ChronoUnit.HOURS);

            List<TransactionRecord> hourRecords = transactionRecordRepository.findAll().stream()
                    .filter(r -> r.getProcessedAt() != null
                            && r.getProcessedAt().isAfter(hourStart)
                            && r.getProcessedAt().isBefore(hourEnd))
                    .collect(Collectors.toList());

            long total = hourRecords.size();
            long fraudulent = hourRecords.stream()
                    .filter(r -> r.getStatus() == TransactionRecord.TransactionStatus.REJECTED)
                    .count();
            double avgScore = hourRecords.stream()
                    .filter(r -> r.getFraudScore() != null)
                    .mapToDouble(TransactionRecord::getFraudScore)
                    .average()
                    .orElse(0.0);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("timestamp", hourEnd.toString());
            point.put("hour", String.format("%02d:00", (24 - i) % 24));
            point.put("totalTransactions", total);
            point.put("fraudulent", fraudulent);
            point.put("avgFraudScore", Math.round(avgScore * 10000.0) / 10000.0);
            trends.add(point);
        }

        return trends;
    }

    /**
     * Get active fraud alerts
     */
    public List<Map<String, Object>> getActiveAlerts() {
        return fraudAlertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::mapAlertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Acknowledge a fraud alert
     */
    public boolean acknowledgeAlert(String alertId, String acknowledgedBy) {
        Optional<FraudAlert> alertOpt = fraudAlertRepository.findByAlertId(alertId);
        if (alertOpt.isPresent()) {
            FraudAlert alert = alertOpt.get();
            alert.setAcknowledged(true);
            alert.setAcknowledgedBy(acknowledgedBy != null ? acknowledgedBy : "dashboard-user");
            alert.setAcknowledgedAt(Instant.now());
            fraudAlertRepository.save(alert);
            log.info("Alert acknowledged: alertId={}", alertId);
            return true;
        }
        return false;
    }

    /**
     * Get AI model status from Python service
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAIStatus() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> status = restTemplate.getForObject(
                    aiServiceUrl + "/api/v1/model/status", Map.class);
            if (status != null) {
                status.put("serviceReachable", true);
                return status;
            }
        } catch (Exception e) {
            log.warn("Could not reach AI service: {}", e.getMessage());
        }

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("serviceReachable", false);
        fallback.put("status", "unavailable");
        fallback.put("model_name", "IsolationForest");
        fallback.put("message", "Python AI service is not reachable. Using fallback rule-based scoring.");
        return fallback;
    }

    private Map<String, Object> mapTransactionToResponse(TransactionRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", record.getId());
        map.put("transactionId", record.getTransactionId());
        map.put("userId", record.getUserId());
        map.put("merchantId", record.getMerchantId());
        map.put("amount", record.getAmount());
        map.put("currency", record.getCurrency());
        map.put("type", record.getType().name());
        map.put("status", record.getStatus().name());
        map.put("fraudScore", record.getFraudScore());
        map.put("fraudReason", record.getFraudReason());
        map.put("processedAt", record.getProcessedAt() != null ? record.getProcessedAt().toString() : null);
        return map;
    }

    private Map<String, Object> mapAlertToResponse(FraudAlert alert) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", alert.getId());
        map.put("alertId", alert.getAlertId());
        map.put("transactionId", alert.getTransactionId());
        map.put("userId", alert.getUserId());
        map.put("alertType", alert.getAlertType().name());
        map.put("severity", alert.getSeverity().name());
        map.put("fraudScore", alert.getFraudScore());
        map.put("details", alert.getDetails());
        map.put("acknowledged", alert.getAcknowledged());
        map.put("createdAt", alert.getCreatedAt() != null ? alert.getCreatedAt().toString() : null);
        return map;
    }
}
