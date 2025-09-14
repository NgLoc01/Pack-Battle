package io.github.ngloc01.pack_battle_app.controller;

import io.github.ngloc01.pack_battle_app.service.RoomManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RoomController {
    private final RoomManager roomManager;
    private final SimpMessagingTemplate messaging;

    public RoomController(RoomManager roomManager, SimpMessagingTemplate messaging) {
        this.roomManager = roomManager;
        this.messaging = messaging;
    }

    @GetMapping("/room/{roomName}")
    public String roomView(@PathVariable String roomName, Model model) {
        System.out.println("Room view requested for: " + roomName);
        model.addAttribute("roomName", roomName);

        switch (roomName.toLowerCase()) {
            case "favoritesroom":
                return "favoritesRoom";
            case "universesbeyondroom":
                return "universesBeyondRoom";
            default:
                System.out.println("Unknown room requested: " + roomName);
                return "error";
        }
    }

    @PostMapping("/room/{roomName}/join")
    @ResponseBody
    public ResponseEntity<?> joinRoom(@PathVariable String roomName, @RequestParam String sessionId) {
        System.out.println("Join request - Room: " + roomName + ", SessionId: " + sessionId);
        
        // Validate session ID
        if (sessionId == null || sessionId.trim().isEmpty()) {
            System.out.println("Invalid session ID provided: " + sessionId);
            return ResponseEntity.badRequest().body("Invalid session ID");
        }
        
        boolean success = roomManager.tryJoin(roomName, sessionId);
        int count = roomManager.userCount(roomName);
        
        System.out.println("Join result - Success: " + success + ", Count: " + count);
        
        // Notify all subscribers of the new user count
        messaging.convertAndSend("/topic/" + roomName + "/count", count);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            System.out.println("Room full - rejecting join request");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Room full");
        }
    }

    @PostMapping("/room/{roomName}/leave")
    @ResponseBody
    public ResponseEntity<?> leaveRoom(@PathVariable String roomName, @RequestParam String sessionId) {
        System.out.println("Leave request - Room: " + roomName + ", SessionId: " + sessionId);
        
        roomManager.leave(roomName, sessionId);
        int count = roomManager.userCount(roomName);
        
        System.out.println("Leave result - Count: " + count);
        
        // Notify all subscribers of the new user count
        messaging.convertAndSend("/topic/" + roomName + "/count", count);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/room/{roomName}/count")
    @ResponseBody
    public int getRoomUserCount(@PathVariable String roomName) {
        int count = roomManager.userCount(roomName);
        System.out.println("Count request - Room: " + roomName + ", Count: " + count);
        return count;
    }
    
    // Debug endpoint to check room status
    @GetMapping("/api/debug/rooms")
    @ResponseBody
    public ResponseEntity<?> debugRooms() {
        return ResponseEntity.ok(roomManager.debugInfo());
    }
}