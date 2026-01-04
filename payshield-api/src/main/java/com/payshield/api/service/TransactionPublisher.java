package com.payshield.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payshield.common.dto.TransactionEvent;
import com.payshield.common.dto.TransactionRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Transaction Publisher Service
 * Publishes incoming transactions to Kafka for async processing
 */
@Service
@Slf4j
public class TransactionPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter publishedCounter;
    private final Counter failedCounter;

    @Value("${payshield.kafka.topics.incoming}")
    private String incomingTopic;

    public TransactionPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;

        this.publishedCounter = Counter.builder("transactions.published")
                .description("Number of transactions published to Kafka")
                .register(meterRegistry);

        this.failedCounter = Counter.builder("transactions.publish.failed")
                .description("Number of failed Kafka publishes")
                .register(meterRegistry);
    }

    /**
     * Publish transaction to Kafka incoming topic
     * Uses transactionId as Kafka key for partition affinity
     */
    public void publishTransaction(String transactionId, TransactionRequest request) {
        TransactionEvent event = TransactionEvent.fromRequest(request, transactionId);

        try {
            String payload = objectMapper.writeValueAsString(event);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(incomingTopic, transactionId,
                    payload);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    failedCounter.increment();
                    log.error("Failed to publish transaction: transactionId={}", transactionId, ex);
                } else {
                    publishedCounter.increment();
                    log.debug("Transaction published: transactionId={}, partition={}, offset={}",
                            transactionId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            failedCounter.increment();
            log.error("Failed to serialize transaction: transactionId={}", transactionId, e);
            throw new RuntimeException("Failed to publish transaction", e);
        }
    }
}
