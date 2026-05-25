package com.security.scanner.config;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
@Import(RabbitAutoConfiguration.class)
public class RabbitProdConfig {

    public static final String SCAN_QUEUE = "scan-queue";

    @Bean
    public Queue scanQueue() {
        return new Queue(SCAN_QUEUE, true);
    }
}
