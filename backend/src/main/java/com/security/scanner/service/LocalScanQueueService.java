package com.security.scanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
public class LocalScanQueueService implements ScanQueueService {

    @Autowired
    private ScanWorker scanWorker;

    @Override
    @Async
    public void queueScan(Long scanId) {
        scanWorker.performScan(scanId);
    }
}
