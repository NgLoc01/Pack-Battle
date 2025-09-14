package io.github.ngloc01.pack_battle_app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomManager roomManager;
  
    @Autowired
    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, RoomManager roomManager) {
        this.messagingTemplate = messagingTemplate;
        this.roomManager = roomManager;
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        
        System.out.println("WebSocket session connected: " + sessionId);
        
        // Send session ID confirmation with multiple strategies
        if (sessionId != null) {
            // Strategy 1: Send to specific session topic (original method with shorter delay)
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Reduced from 2000ms to 500ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                messagingTemplate.convertAndSend("/topic/session/" + sessionId, sessionId);
                System.out.println("Sent session ID to /topic/session/" + sessionId);
            }).start();
            
            // Strategy 2: Broadcast session ID (for fallback)
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Send broadcast after individual message
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                messagingTemplate.convertAndSend("/topic/session-broadcast", sessionId);
                System.out.println("Broadcasted session ID: " + sessionId);
            }).start();
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        
        System.out.println("WebSocket session disconnected: " + sessionId);
        
        if (sessionId != null) {
            // Remove user from all rooms they might be in
            removeUserFromAllRooms(sessionId);
        }
    }
    
    private void removeUserFromAllRooms(String sessionId) {
        // Remove from both rooms and update counts
        String[] roomNames = {"favoritesRoom", "universesBeyondRoom"};
        
        for (String roomName : roomNames) {
            roomManager.leave(roomName, sessionId);
            int count = roomManager.userCount(roomName);
            messagingTemplate.convertAndSend("/topic/" + roomName + "/count", count);
            System.out.println("Removed session " + sessionId + " from " + roomName + ", new count: " + count);
        }
    }
}