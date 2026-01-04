package com.payshield.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * PayShield API Gateway
 * Receives REST requests and publishes to Kafka for async processing
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableMongoRepositories(basePackages = "com.payshield.common.repository")
public class PayShieldApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayShieldApiApplication.class, args);
    }
}
