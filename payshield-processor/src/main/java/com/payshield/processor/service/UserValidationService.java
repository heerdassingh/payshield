package com.payshield.processor.service;

import com.payshield.common.dto.TransactionEvent;
import com.payshield.common.entity.Merchant;
import com.payshield.common.entity.User;
import com.payshield.common.repository.MerchantRepository;
import com.payshield.common.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * User Validation Service
 * Validates users and merchants against MySQL master data
 */
@Service
@Slf4j
public class UserValidationService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final Counter userValidCounter;
    private final Counter userInvalidCounter;
    private final Counter merchantInvalidCounter;

    public UserValidationService(
            UserRepository userRepository,
            MerchantRepository merchantRepository,
            MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.merchantRepository = merchantRepository;

        this.userValidCounter = Counter.builder("validation.user.valid")
                .description("Valid user validations")
                .register(meterRegistry);

        this.userInvalidCounter = Counter.builder("validation.user.invalid")
                .description("Invalid user validations")
                .register(meterRegistry);

        this.merchantInvalidCounter = Counter.builder("validation.merchant.invalid")
                .description("Invalid merchant validations")
                .register(meterRegistry);
    }

    /**
     * Validate user exists and is active
     */
    public ValidationResult validateUser(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            userInvalidCounter.increment();
            log.warn("User not found: userId={}", userId);
            return ValidationResult.userNotFound(userId);
        }

        User user = userOpt.get();

        if (user.isBlocked()) {
            userInvalidCounter.increment();
            log.warn("User is blocked: userId={}", userId);
            return ValidationResult.userBlocked(userId);
        }

        if (!user.isActive()) {
            userInvalidCounter.increment();
            log.warn("User is not active: userId={}, status={}", userId, user.getStatus());
            return ValidationResult.userInactive(userId, user.getStatus().name());
        }

        userValidCounter.increment();
        log.debug("User validated successfully: userId={}", userId);
        return ValidationResult.success(user);
    }

    /**
     * Validate merchant exists and is active
     */
    public ValidationResult validateMerchant(String merchantId) {
        Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);

        if (merchantOpt.isEmpty()) {
            merchantInvalidCounter.increment();
            log.warn("Merchant not found: merchantId={}", merchantId);
            return ValidationResult.merchantNotFound(merchantId);
        }

        Merchant merchant = merchantOpt.get();

        if (!merchant.isActive()) {
            merchantInvalidCounter.increment();
            log.warn("Merchant is not active: merchantId={}, status={}", merchantId, merchant.getStatus());
            return ValidationResult.merchantInactive(merchantId, merchant.getStatus().name());
        }

        log.debug("Merchant validated: merchantId={}, riskLevel={}", merchantId, merchant.getRiskLevel());
        return ValidationResult.success(merchant);
    }

    /**
     * Full validation for a transaction event
     */
    public TransactionEvent validateTransaction(TransactionEvent event) {
        // Validate user
        ValidationResult userResult = validateUser(event.getUserId());
        if (!userResult.isValid()) {
            event.setUserValid(false);
            event.setValidationError(userResult.getError());
            event.setEventType(TransactionEvent.EventType.USER_NOT_FOUND);
            event.setStage("USER_VALIDATION_FAILED");
            return event;
        }

        // Validate merchant
        ValidationResult merchantResult = validateMerchant(event.getMerchantId());
        if (!merchantResult.isValid()) {
            event.setMerchantValid(false);
            event.setValidationError(merchantResult.getError());
            event.setEventType(TransactionEvent.EventType.FAILED);
            event.setStage("MERCHANT_VALIDATION_FAILED");
            return event;
        }

        // Check transaction limits
        User user = (User) userResult.getEntity();
        if (event.getAmount().compareTo(user.getMaxTransactionLimit()) > 0) {
            event.setUserValid(true);
            event.setMerchantValid(true);
            event.setValidationError("Transaction exceeds user limit: max=" + user.getMaxTransactionLimit());
            event.setEventType(TransactionEvent.EventType.FAILED);
            event.setStage("LIMIT_EXCEEDED");
            return event;
        }

        // Validation passed
        event.setUserValid(true);
        event.setMerchantValid(true);
        event.setEventType(TransactionEvent.EventType.USER_VALIDATED);
        event.setStage("VALIDATED");
        log.info("Transaction validated: transactionId={}", event.getTransactionId());
        return event;
    }

    /**
     * Validation result holder
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String error;
        private final Object entity;

        private ValidationResult(boolean valid, String error, Object entity) {
            this.valid = valid;
            this.error = error;
            this.entity = entity;
        }

        public static ValidationResult success(Object entity) {
            return new ValidationResult(true, null, entity);
        }

        public static ValidationResult userNotFound(String userId) {
            return new ValidationResult(false, "User not found: " + userId, null);
        }

        public static ValidationResult userBlocked(String userId) {
            return new ValidationResult(false, "User is blocked: " + userId, null);
        }

        public static ValidationResult userInactive(String userId, String status) {
            return new ValidationResult(false, "User is inactive: " + userId + " (status: " + status + ")", null);
        }

        public static ValidationResult merchantNotFound(String merchantId) {
            return new ValidationResult(false, "Merchant not found: " + merchantId, null);
        }

        public static ValidationResult merchantInactive(String merchantId, String status) {
            return new ValidationResult(false, "Merchant is inactive: " + merchantId + " (status: " + status + ")", null);
        }

        public boolean isValid() { return valid; }
        public String getError() { return error; }
        public Object getEntity() { return entity; }
    }
}
