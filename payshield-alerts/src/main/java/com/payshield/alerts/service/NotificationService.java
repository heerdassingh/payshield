package com.payshield.alerts.service;

import com.payshield.common.dto.TransactionEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Notification Service - Sends alerts via various channels
 * Currently logs to console; can be extended for email, Slack, etc.
 */
@Service
@Slf4j
public class NotificationService {

    private final Counter notificationsSentCounter;

    @Value("${payshield.notifications.enabled:true}")
    private boolean notificationsEnabled;

    public NotificationService(MeterRegistry meterRegistry) {
        this.notificationsSentCounter = Counter.builder("notifications.sent")
                .description("Number of notifications sent")
                .register(meterRegistry);
    }

    public void sendFraudAlert(TransactionEvent event) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled, skipping");
            return;
        }

        // Log alert (in production, send to Slack/Email/PagerDuty)
        log.error("""

                ╔══════════════════════════════════════════════════════════════╗
                ║                     🚨 FRAUD ALERT 🚨                        ║
                ╠══════════════════════════════════════════════════════════════╣
                ║ Transaction ID: {}
                ║ User ID:        {}
                ║ Merchant ID:    {}
                ║ Amount:         {} {}
                ║ Fraud Score:    {}
                ║ Reason:         {}
                ╚══════════════════════════════════════════════════════════════╝
                """,
                event.getTransactionId(),
                event.getUserId(),
                event.getMerchantId(),
                event.getAmount(), event.getCurrency(),
                String.format("%.2f", event.getFraudScore()),
                event.getFraudReason());

        notificationsSentCounter.increment();

        // TODO: Implement actual notification channels
        // sendSlackNotification(event);
        // sendEmailNotification(event);
        // sendPagerDutyAlert(event);
    }

    public void sendUserNotFoundAlert(TransactionEvent event) {
        if (!notificationsEnabled) {
            return;
        }

        log.warn("""

                ╔══════════════════════════════════════════════════════════════╗
                ║                  ⚠️ USER NOT FOUND ⚠️                         ║
                ╠══════════════════════════════════════════════════════════════╣
                ║ Transaction ID: {}
                ║ User ID:        {}
                ║ Error:          {}
                ╚══════════════════════════════════════════════════════════════╝
                """,
                event.getTransactionId(),
                event.getUserId(),
                event.getValidationError());

        notificationsSentCounter.increment();
    }

    public void sendRetryExhaustedAlert(String transactionId, int retryCount, String reason) {
        if (!notificationsEnabled) {
            return;
        }

        log.error("""

                ╔══════════════════════════════════════════════════════════════╗
                ║               💀 RETRY EXHAUSTED - CRITICAL 💀               ║
                ╠══════════════════════════════════════════════════════════════╣
                ║ Transaction ID: {}
                ║ Retry Count:    {}
                ║ Failure Reason: {}
                ╚══════════════════════════════════════════════════════════════╝
                """,
                transactionId,
                retryCount,
                reason);

        notificationsSentCounter.increment();
    }
}
