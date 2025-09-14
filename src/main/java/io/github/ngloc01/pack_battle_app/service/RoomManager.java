package io.github.ngloc01.pack_battle_app.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomManager {
    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();
    /*
     * Example structure of rooms map:
     * "favoritesRoom": [ "sess-abc123", "sess-def456" ],
     * "universesBeyondRoom": [ "sess-xyz789" ]
     */
   
    public synchronized boolean tryJoin(String roomName, String sessionId) {
        rooms.putIfAbsent(roomName, ConcurrentHashMap.newKeySet());
        Set<String> users = rooms.get(roomName); // Get or create the set of users for the room
        if (users.size() >= 2) return false;
        users.add(sessionId); // Add the session ID to the room
        return true;
    }

    public synchronized void leave(String roomName, String sessionId) {
        Set<String> users = rooms.get(roomName); // Get the set of users for the room
        if (users != null) {
            users.remove(sessionId);    // Remove the session ID from the room
            if (users.isEmpty()) {      // If the room is empty, remove it from the rooms hashmap
                rooms.remove(roomName); // Clean up empty rooms
            }
        }
    }

    public synchronized int userCount(String roomName) {
        return rooms.getOrDefault(roomName, Set.of()).size();
    }
}

