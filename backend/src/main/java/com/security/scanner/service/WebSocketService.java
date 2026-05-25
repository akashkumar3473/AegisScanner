package com.security.scanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendScanUpdate(Long scanId, String status, String step, Map<String, Object> extra) {
        String destination = "/topic/scans/" + scanId;
        Map<String, Object> payload = new HashMap<>();
        payload.put("scanId", scanId);
        payload.put("status", status);
        payload.put("step", step);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("extra", extra != null ? extra : new HashMap<>());

        messagingTemplate.convertAndSend(destination, payload);
    }
}
