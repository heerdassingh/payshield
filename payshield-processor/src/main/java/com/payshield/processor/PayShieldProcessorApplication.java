package com.payshield.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PayShield Transaction Processor
 * - Consumes transactions from Kafka
 * - Validates users against MySQL
 * - Runs AI fraud detection (Java ML)
 * - Stores results in MongoDB
 * - Retries failed transactions
 */
@SpringBootApplication(scanBasePackages = {"com.payshield.processor", "com.payshield.common"})
@EnableScheduling
@EntityScan(basePackages = "com.payshield.common.entity")
@EnableJpaRepositories(basePackages = "com.payshield.common.repository")
@EnableMongoRepositories(basePackages = "com.payshield.common.repository")
public class PayShieldProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayShieldProcessorApplication.class, args);
    }
}
