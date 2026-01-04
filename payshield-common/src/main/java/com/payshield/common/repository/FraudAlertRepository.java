package com.payshield.common.repository;

import com.payshield.common.model.FraudAlert;
import com.payshield.common.model.FraudAlert.AlertType;
import com.payshield.common.model.FraudAlert.Severity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository - Fraud Alerts
 */
@Repository
public interface FraudAlertRepository extends MongoRepository<FraudAlert, String> {

    Optional<FraudAlert> findByAlertId(String alertId);

    List<FraudAlert> findByTransactionId(String transactionId);

    List<FraudAlert> findBySeverityAndAcknowledgedFalse(Severity severity);

    List<FraudAlert> findByAlertTypeAndCreatedAtAfter(AlertType alertType, Instant after);

    List<FraudAlert> findByAcknowledgedFalseOrderByCreatedAtDesc();

    long countBySeverityAndAcknowledgedFalse(Severity severity);

    long countByCreatedAtAfter(Instant after);
}
