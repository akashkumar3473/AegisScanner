package com.security.scanner.service;

import com.security.scanner.config.RabbitProdConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class RabbitScanListener {

    @Autowired
    private ScanWorker scanWorker;

    @RabbitListener(queues = RabbitProdConfig.SCAN_QUEUE)
    public void receiveScanMessage(Long scanId) {
        scanWorker.performScan(scanId);
    }
}
