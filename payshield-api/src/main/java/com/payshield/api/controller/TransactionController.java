package com.payshield.api.controller;

import com.payshield.api.service.TransactionPublisher;
import com.payshield.common.dto.TransactionRequest;
import com.payshield.common.dto.TransactionResponse;
import com.payshield.common.dto.TransactionStatus;
import com.payshield.common.model.TransactionRecord;
import com.payshield.common.repository.TransactionRecordRepository;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Transaction API Controller
 * Receives requests and immediately publishes to Kafka (NO sync processing)
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Transaction submission and status endpoints")
public class TransactionController {

        private final TransactionPublisher transactionPublisher;
        private final TransactionRecordRepository transactionRecordRepository;

        /**
         * Submit a transaction for async processing
         * Immediately publishes to Kafka and returns transaction ID
         */
        @PostMapping
        @Timed(value = "transactions.submit", description = "Time to submit transaction")
        @Operation(summary = "Submit transaction", description = "Submit a transaction for async processing via Kafka")
        public ResponseEntity<TransactionResponse> submitTransaction(
                        @Valid @RequestBody TransactionRequest request) {

                String transactionId = UUID.randomUUID().toString();
                log.info("Received transaction request: transactionId={}, userId={}, amount={}",
                                transactionId, request.getUserId(), request.getAmount());

                try {
                        // Publish to Kafka immediately - no sync processing!
                        transactionPublisher.publishTransaction(transactionId, request);

                        TransactionResponse response = TransactionResponse.accepted(
                                        transactionId,
                                        request.getAmount(),
                                        request.getCurrency());

                        log.info("Transaction published to Kafka: transactionId={}", transactionId);
                        return ResponseEntity.accepted().body(response);

                } catch (Exception e) {
                        log.error("Failed to publish transaction: transactionId={}", transactionId, e);
                        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                        .body(TransactionResponse
                                                        .rejected("Failed to submit transaction: " + e.getMessage()));
                }
        }

        /**
         * Get transaction status by ID
         */
        @GetMapping("/{transactionId}/status")
        @Operation(summary = "Get transaction status", description = "Check the processing status of a transaction")
        public ResponseEntity<TransactionStatus> getTransactionStatus(
                        @PathVariable String transactionId) {

                return transactionRecordRepository.findByTransactionId(transactionId)
                                .map(record -> ResponseEntity.ok(mapToStatus(record)))
                                .orElse(ResponseEntity.ok(TransactionStatus.builder()
                                                .transactionId(transactionId)
                                                .status("PROCESSING")
                                                .stage("IN_PIPELINE")
                                                .message("Transaction is being processed")
                                                .build()));
        }

        /**
         * Get user's transaction history
         */
        @GetMapping("/user/{userId}")
        @Operation(summary = "Get user transactions", description = "Get all transactions for a user")
        public ResponseEntity<List<TransactionRecord>> getUserTransactions(
                        @PathVariable String userId) {

                List<TransactionRecord> transactions = transactionRecordRepository
                                .findByUserIdOrderByProcessedAtDesc(userId);
                return ResponseEntity.ok(transactions);
        }

        private TransactionStatus mapToStatus(TransactionRecord record) {
                return TransactionStatus.builder()
                                .transactionId(record.getTransactionId())
                                .status(record.getStatus().name())
                                .stage("COMPLETED")
                                .fraudScore(record.getFraudScore())
                                .fraudReason(record.getFraudReason())
                                .isFraudulent(record.getFraudScore() != null && record.getFraudScore() > 0.7)
                                .processedAt(record.getProcessedAt())
                                .build();
        }
}
