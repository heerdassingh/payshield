package com.payshield.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * PayShield Alerts Service
 * Consumes fraud alerts and sends notifications
 */
@SpringBootApplication(scanBasePackages = { "com.payshield.alerts", "com.payshield.common" }, exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableMongoRepositories(basePackages = "com.payshield.common.repository")
public class PayShieldAlertsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayShieldAlertsApplication.class, args);
    }
}
