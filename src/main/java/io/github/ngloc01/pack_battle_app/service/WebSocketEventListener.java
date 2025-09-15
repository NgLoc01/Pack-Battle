package io.github.ngloc01.pack_battle_app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;

@Component
public class WebSocketEventListener {
    private final SimpMessagingTemplate messagingTemplate;
  
    @Autowired
    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // Delay sending the session ID to give the client time to subscribe
        new Thread(() -> {
            try {
                Thread.sleep(200); // Wait 200ms to avoid race condition
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, sessionId); //scripts.js
        }).start();
    }

    
    /* @EventListener //dont work cant close tab
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
    
    }  */

}
