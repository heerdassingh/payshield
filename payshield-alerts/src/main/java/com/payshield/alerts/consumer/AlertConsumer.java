package com.payshield.alerts.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payshield.alerts.service.NotificationService;
import com.payshield.common.dto.TransactionEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Alert Consumer - Consumes fraud alerts and user-not-found events
 */
@Component
@Slf4j
public class AlertConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final Counter fraudAlertsCounter;
    private final Counter userNotFoundCounter;

    public AlertConsumer(
            NotificationService notificationService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;

        this.fraudAlertsCounter = Counter.builder("alerts.fraud.received")
                .description("Fraud alerts received")
                .register(meterRegistry);

        this.userNotFoundCounter = Counter.builder("alerts.user_not_found.received")
                .description("User not found alerts received")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "${payshield.kafka.topics.fraud-alerts}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeFraudAlert(String message) {
        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            fraudAlertsCounter.increment();

            log.warn("🚨 FRAUD ALERT: transactionId={}, userId={}, amount={}, score={}, reason={}",
                    event.getTransactionId(),
                    event.getUserId(),
                    event.getAmount(),
                    event.getFraudScore(),
                    event.getFraudReason());

            // Send notifications
            notificationService.sendFraudAlert(event);

        } catch (Exception e) {
            log.error("Failed to process fraud alert", e);
        }
    }

    @KafkaListener(topics = "${payshield.kafka.topics.user-not-found}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUserNotFound(String message) {
        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            userNotFoundCounter.increment();

            log.warn("⚠️ USER NOT FOUND: transactionId={}, userId={}, error={}",
                    event.getTransactionId(),
                    event.getUserId(),
                    event.getValidationError());

            // Send notification
            notificationService.sendUserNotFoundAlert(event);

        } catch (Exception e) {
            log.error("Failed to process user-not-found alert", e);
        }
    }
}
