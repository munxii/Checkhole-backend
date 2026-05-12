package com.example.SmartHelmet.websocket;

import com.example.SmartHelmet.dto.AlertResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /** Broadcasts to all connected clients. Frontend filters by visible regions. */
    public void publish(AlertResponse payload) {
        messagingTemplate.convertAndSend("/topic/alerts", payload);
    }
}
