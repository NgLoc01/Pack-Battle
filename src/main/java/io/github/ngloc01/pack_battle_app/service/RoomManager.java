package io.github.ngloc01.pack_battle_app.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomManager {
    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();
    
    public synchronized boolean tryJoin(String roomName, String sessionId) {
        System.out.println("tryJoin called - Room: " + roomName + ", Session: " + sessionId);
        
        rooms.putIfAbsent(roomName, ConcurrentHashMap.newKeySet());
        Set<String> users = rooms.get(roomName);
        
        // Check if user is already in the room
        if (users.contains(sessionId)) {
            System.out.println("User already in room: " + sessionId);
            return true; // Already joined, consider it successful
        }
        
        if (users.size() >= 2) {
            System.out.println("Room full - Current users: " + users);
            return false;
        }
        
        users.add(sessionId);
        System.out.println("User added to room. Current users: " + users);
        return true;
    }

    public synchronized void leave(String roomName, String sessionId) {
        System.out.println("leave called - Room: " + roomName + ", Session: " + sessionId);
        
        Set<String> users = rooms.get(roomName);
        if (users != null) {
            boolean removed = users.remove(sessionId);
            System.out.println("User removal result: " + removed + ", Remaining users: " + users);
            
            if (users.isEmpty()) {
                rooms.remove(roomName);
                System.out.println("Room removed as it's empty: " + roomName);
            }
        } else {
            System.out.println("Room not found: " + roomName);
        }
    }

    public synchronized int userCount(String roomName) {
        int count = rooms.getOrDefault(roomName, Set.of()).size();
        System.out.println("userCount called - Room: " + roomName + ", Count: " + count);
        return count;
    }
    
    // Debug method to see current room state
    public synchronized Map<String, Object> debugInfo() {
        Map<String, Object> debug = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : rooms.entrySet()) {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("users", entry.getValue());
            roomInfo.put("count", entry.getValue().size());
            debug.put(entry.getKey(), roomInfo);
        }
        
        System.out.println("Debug info requested: " + debug);
        return debug;
    }
}