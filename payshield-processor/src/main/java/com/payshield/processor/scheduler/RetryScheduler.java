package com.payshield.processor.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payshield.common.dto.TransactionEvent;
import com.payshield.common.model.FailedTransaction;
import com.payshield.common.model.FraudAlert;
import com.payshield.common.repository.FailedTransactionRepository;
import com.payshield.common.repository.FraudAlertRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Retry Scheduler - Periodically retries failed transactions
 * Uses exponential backoff: 2^retryCount * 60 seconds
 */
@Component
@Slf4j
public class RetryScheduler {

    private final FailedTransactionRepository failedTransactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final Counter retriedCounter;
    private final Counter exhaustedCounter;

    @Value("${payshield.kafka.topics.incoming}")
    private String incomingTopic;

    @Value("${payshield.retry.max-retries:5}")
    private int maxRetries;

    public RetryScheduler(
            FailedTransactionRepository failedTransactionRepository,
            FraudAlertRepository fraudAlertRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.failedTransactionRepository = failedTransactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;

        this.retriedCounter = Counter.builder("retry.attempted")
                .description("Number of retry attempts")
                .register(meterRegistry);

        this.exhaustedCounter = Counter.builder("retry.exhausted")
                .description("Number of exhausted retries")
                .register(meterRegistry);

        // Gauge for pending retries
        Gauge.builder("retry.pending", failedTransactionRepository,
                repo -> repo.countByStatus(FailedTransaction.RetryStatus.PENDING_RETRY))
                .description("Number of transactions pending retry")
                .register(meterRegistry);
    }

    /**
     * Runs every 60 seconds to check for transactions to retry
     */
    @Scheduled(fixedDelayString = "${payshield.retry.interval-ms:60000}")
    public void retryFailedTransactions() {
        log.debug("Running retry scheduler...");

        Instant now = Instant.now();

        // Find transactions ready for retry
        List<FailedTransaction> toRetry = failedTransactionRepository
                .findByStatusAndNextRetryAtBeforeAndRetryCountLessThan(
                        FailedTransaction.RetryStatus.PENDING_RETRY,
                        now,
                        maxRetries
                );

        if (toRetry.isEmpty()) {
            log.debug("No transactions to retry");
            return;
        }

        log.info("Found {} transactions to retry", toRetry.size());

        for (FailedTransaction failed : toRetry) {
            retryTransaction(failed);
        }
    }

    /**
     * Retry a single failed transaction
     */
    private void retryTransaction(FailedTransaction failed) {
        String transactionId = failed.getTransactionId();
        log.info("Retrying transaction: transactionId={}, attempt={}/{}",
                transactionId, failed.getRetryCount() + 1, failed.getMaxRetries());

        try {
            // Mark as retrying
            failed.setStatus(FailedTransaction.RetryStatus.RETRYING);
            failedTransactionRepository.save(failed);

            // Reconstruct the event from original payload
            TransactionEvent event = objectMapper.convertValue(
                    failed.getOriginalPayload(), TransactionEvent.class);

            // Reset event state for reprocessing
            event.setEventType(TransactionEvent.EventType.INCOMING);
            event.setStage("RETRY_" + (failed.getRetryCount() + 1));

            // Publish back to incoming topic
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(incomingTopic, transactionId, payload);

            // Update retry count and schedule next retry
            failed.incrementRetry();
            failed.setStatus(FailedTransaction.RetryStatus.PENDING_RETRY);
            failedTransactionRepository.save(failed);

            retriedCounter.increment();
            log.info("Transaction republished for retry: transactionId={}, nextRetry={}",
                    transactionId, failed.getNextRetryAt());

        } catch (Exception e) {
            log.error("Failed to retry transaction: transactionId={}", transactionId, e);

            // Mark as retrying failed
            failed.incrementRetry();
            if (failed.getRetryCount() >= failed.getMaxRetries()) {
                handleRetryExhausted(failed);
            } else {
                failed.setStatus(FailedTransaction.RetryStatus.PENDING_RETRY);
                failedTransactionRepository.save(failed);
            }
        }
    }

    /**
     * Handle when all retries are exhausted
     */
    private void handleRetryExhausted(FailedTransaction failed) {
        log.error("RETRY EXHAUSTED: transactionId={}, attempts={}",
                failed.getTransactionId(), failed.getRetryCount());

        failed.setStatus(FailedTransaction.RetryStatus.EXHAUSTED);
        failedTransactionRepository.save(failed);

        exhaustedCounter.increment();

        // Create critical alert
        FraudAlert alert = FraudAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .transactionId(failed.getTransactionId())
                .alertType(FraudAlert.AlertType.RETRY_EXHAUSTED)
                .severity(FraudAlert.Severity.CRITICAL)
                .details(Map.of(
                        "retryCount", failed.getRetryCount().toString(),
                        "failureReason", failed.getFailureReason().name(),
                        "errorDetails", failed.getErrorDetails() != null ? failed.getErrorDetails() : "Unknown"
                ))
                .createdAt(Instant.now())
                .build();

        fraudAlertRepository.save(alert);
        log.warn("Created CRITICAL alert for exhausted retry: transactionId={}", failed.getTransactionId());
    }

    /**
     * Cleanup old exhausted records (runs daily)
     */
    @Scheduled(cron = "0 0 3 * * ?") // 3 AM daily
    public void cleanupExhaustedRecords() {
        log.info("Running cleanup of exhausted retry records...");
        // In production, move to archive or delete old records
        long count = failedTransactionRepository.countByStatus(FailedTransaction.RetryStatus.EXHAUSTED);
        log.info("Found {} exhausted retry records", count);
    }
}
