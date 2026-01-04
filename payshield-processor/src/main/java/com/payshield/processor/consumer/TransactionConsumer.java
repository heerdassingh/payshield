package com.payshield.processor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payshield.common.dto.TransactionEvent;
import com.payshield.common.model.FailedTransaction;
import com.payshield.common.model.FraudAlert;
import com.payshield.common.model.TransactionRecord;
import com.payshield.common.repository.FailedTransactionRepository;
import com.payshield.common.repository.FraudAlertRepository;
import com.payshield.common.repository.TransactionRecordRepository;
import com.payshield.processor.service.FraudDetectionService;
import com.payshield.processor.service.UserValidationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Transaction Consumer - Orchestrates the processing pipeline
 * 1. Receives from incoming-transactions
 * 2. Validates user (MySQL)
 * 3. Runs fraud detection (Java ML)
 * 4. Stores result or failure for retry
 */
@Component
@Slf4j
public class TransactionConsumer {

    private final UserValidationService userValidationService;
    private final FraudDetectionService fraudDetectionService;
    private final TransactionRecordRepository transactionRecordRepository;
    private final FailedTransactionRepository failedTransactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final Counter processedCounter;
    private final Counter failedCounter;
    private final Timer processingTimer;

    @Value("${payshield.kafka.topics.fraud-alerts}")
    private String fraudAlertsTopic;

    @Value("${payshield.kafka.topics.user-not-found}")
    private String userNotFoundTopic;

    public TransactionConsumer(
            UserValidationService userValidationService,
            FraudDetectionService fraudDetectionService,
            TransactionRecordRepository transactionRecordRepository,
            FailedTransactionRepository failedTransactionRepository,
            FraudAlertRepository fraudAlertRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.userValidationService = userValidationService;
        this.fraudDetectionService = fraudDetectionService;
        this.transactionRecordRepository = transactionRecordRepository;
        this.failedTransactionRepository = failedTransactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;

        this.processedCounter = Counter.builder("transactions.processed")
                .description("Successfully processed transactions")
                .register(meterRegistry);

        this.failedCounter = Counter.builder("transactions.failed")
                .description("Failed transactions")
                .register(meterRegistry);

        this.processingTimer = Timer.builder("transactions.processing.time")
                .description("Transaction processing time")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = "${payshield.kafka.topics.incoming}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransaction(String message, Acknowledgment ack) {
        processingTimer.record(() -> {
            TransactionEvent event = null;
            try {
                event = objectMapper.readValue(message, TransactionEvent.class);
                log.info("Processing transaction: transactionId={}", event.getTransactionId());

                // Check idempotency - skip if already processed
                if (transactionRecordRepository.existsByTransactionId(event.getTransactionId())) {
                    log.info("Transaction already processed (idempotency check): transactionId={}",
                            event.getTransactionId());
                    ack.acknowledge();
                    return;
                }

                // Step 1: Validate User
                event = userValidationService.validateTransaction(event);

                if (!Boolean.TRUE.equals(event.getUserValid())) {
                    handleUserNotFound(event);
                    ack.acknowledge();
                    return;
                }

                // Step 2: AI Fraud Detection
                event = fraudDetectionService.detectFraud(event);

                if (Boolean.TRUE.equals(event.getIsFraudulent())) {
                    handleFraudDetected(event);
                    ack.acknowledge();
                    return;
                }

                // Step 3: Store successful transaction
                storeSuccessfulTransaction(event);
                processedCounter.increment();

                log.info("Transaction completed: transactionId={}", event.getTransactionId());
                ack.acknowledge();

            } catch (Exception e) {
                failedCounter.increment();
                log.error("Failed to process transaction: {}", e.getMessage(), e);

                if (event != null) {
                    storeFailedTransaction(event, FailedTransaction.FailureReason.SYSTEM_ERROR, e.getMessage());
                }

                // Acknowledge to avoid infinite retry (we handle retry ourselves)
                ack.acknowledge();
            }
        });
    }

    private void handleUserNotFound(TransactionEvent event) {
        log.warn("User validation failed: transactionId={}, error={}",
                event.getTransactionId(), event.getValidationError());

        // Publish to user-not-found topic
        try {
            kafkaTemplate.send(userNotFoundTopic, event.getTransactionId(),
                    objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish user-not-found event", e);
        }

        // Create alert
        createAlert(event, FraudAlert.AlertType.USER_NOT_FOUND, FraudAlert.Severity.MEDIUM);

        failedCounter.increment();
    }

    private void handleFraudDetected(TransactionEvent event) {
        log.warn("FRAUD DETECTED: transactionId={}, score={}, reason={}",
                event.getTransactionId(), event.getFraudScore(), event.getFraudReason());

        // Store as rejected transaction
        TransactionRecord record = TransactionRecord.builder()
                .transactionId(event.getTransactionId())
                .userId(event.getUserId())
                .merchantId(event.getMerchantId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .type(TransactionRecord.TransactionType.valueOf(event.getType()))
                .status(TransactionRecord.TransactionStatus.REJECTED)
                .fraudScore(event.getFraudScore())
                .fraudReason(event.getFraudReason())
                .processedAt(Instant.now())
                .build();
        transactionRecordRepository.save(record);

        // Publish to fraud-alerts topic
        try {
            kafkaTemplate.send(fraudAlertsTopic, event.getTransactionId(),
                    objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish fraud alert", e);
        }

        // Create high-severity alert
        FraudAlert.Severity severity = event.getFraudScore() > 0.9 ?
                FraudAlert.Severity.CRITICAL : FraudAlert.Severity.HIGH;
        createAlert(event, FraudAlert.AlertType.FRAUD_DETECTED, severity);

        failedCounter.increment();
    }

    private void storeSuccessfulTransaction(TransactionEvent event) {
        TransactionRecord record = TransactionRecord.builder()
                .transactionId(event.getTransactionId())
                .userId(event.getUserId())
                .merchantId(event.getMerchantId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .type(TransactionRecord.TransactionType.valueOf(event.getType()))
                .status(TransactionRecord.TransactionStatus.COMPLETED)
                .fraudScore(event.getFraudScore())
                .processedAt(Instant.now())
                .build();

        transactionRecordRepository.save(record);
        log.debug("Transaction stored: transactionId={}", event.getTransactionId());
    }

    private void storeFailedTransaction(TransactionEvent event,
                                        FailedTransaction.FailureReason reason,
                                        String errorDetails) {
        try {
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);

            FailedTransaction failed = FailedTransaction.builder()
                    .transactionId(event.getTransactionId())
                    .originalPayload(payload)
                    .failureReason(reason)
                    .errorDetails(errorDetails)
                    .retryCount(0)
                    .maxRetries(5)
                    .status(FailedTransaction.RetryStatus.PENDING_RETRY)
                    .nextRetryAt(Instant.now().plusSeconds(60))
                    .createdAt(Instant.now())
                    .build();

            failedTransactionRepository.save(failed);
            log.info("Stored failed transaction for retry: transactionId={}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to store failed transaction", e);
        }
    }

    private void createAlert(TransactionEvent event, FraudAlert.AlertType type, FraudAlert.Severity severity) {
        try {
            FraudAlert alert = FraudAlert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .transactionId(event.getTransactionId())
                    .userId(event.getUserId())
                    .alertType(type)
                    .severity(severity)
                    .fraudScore(event.getFraudScore())
                    .details(Map.of(
                            "merchantId", event.getMerchantId(),
                            "amount", event.getAmount().toString(),
                            "reason", event.getFraudReason() != null ? event.getFraudReason() : event.getValidationError()
                    ))
                    .createdAt(Instant.now())
                    .build();

            fraudAlertRepository.save(alert);
            log.debug("Alert created: alertId={}, type={}", alert.getAlertId(), type);

        } catch (Exception e) {
            log.error("Failed to create alert", e);
        }
    }
}
