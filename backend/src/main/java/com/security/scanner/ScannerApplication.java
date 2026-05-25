package com.security.scanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { RabbitAutoConfiguration.class })
@EnableAsync
@EnableScheduling
public class ScannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScannerApplication.class, args);
    }
}
