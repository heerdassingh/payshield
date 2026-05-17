package com.payshield.api.controller;

import com.payshield.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dashboard API Controller — Serves data for the React frontend
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Dashboard endpoints for fraud analytics visualization")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get overall dashboard statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get dashboard stats", description = "Overall system statistics for the dashboard")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    /**
     * Get recent transactions
     */
    @GetMapping("/recent-transactions")
    @Operation(summary = "Get recent transactions", description = "Most recent transactions with fraud scores")
    public ResponseEntity<List<Map<String, Object>>> getRecentTransactions(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentTransactions(limit));
    }

    /**
     * Get fraud trends over time
     */
    @GetMapping("/fraud-trends")
    @Operation(summary = "Get fraud trends", description = "Fraud score trends over last 24 hours")
    public ResponseEntity<List<Map<String, Object>>> getFraudTrends() {
        return ResponseEntity.ok(dashboardService.getFraudTrends());
    }

    /**
     * Get active fraud alerts
     */
    @GetMapping("/alerts")
    @Operation(summary = "Get active alerts", description = "All unacknowledged fraud alerts")
    public ResponseEntity<List<Map<String, Object>>> getActiveAlerts() {
        return ResponseEntity.ok(dashboardService.getActiveAlerts());
    }

    /**
     * Acknowledge a fraud alert
     */
    @PostMapping("/alerts/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge alert", description = "Mark a fraud alert as acknowledged")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(
            @PathVariable String alertId,
            @RequestParam(required = false) String acknowledgedBy) {
        boolean success = dashboardService.acknowledgeAlert(alertId, acknowledgedBy);
        if (success) {
            return ResponseEntity.ok(Map.of("status", "acknowledged", "alertId", alertId));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get AI model status
     */
    @GetMapping("/ai-status")
    @Operation(summary = "Get AI status", description = "Status of the Python AI fraud detection model")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        return ResponseEntity.ok(dashboardService.getAIStatus());
    }
}
