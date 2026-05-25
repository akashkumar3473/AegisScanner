package com.security.scanner.service;

import com.security.scanner.config.RabbitProdConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class RabbitScanQueueService implements ScanQueueService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void queueScan(Long scanId) {
        rabbitTemplate.convertAndSend(RabbitProdConfig.SCAN_QUEUE, scanId);
    }
}
