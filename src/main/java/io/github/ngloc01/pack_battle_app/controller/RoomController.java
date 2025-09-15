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

    @GetMapping("/room/{roomName}") //Call comes form window.location.href = `/room/${roomName}` and sends the user to the right room view
    public String roomView(@PathVariable String roomName, Model model) {
        model.addAttribute("roomName", roomName); //roomName is passed 

        switch (roomName.toLowerCase()) {
            case "favoritesroom":
                return "favoritesRoom"; //loads favoritesRoom.html
            case "universesbeyondroom":
                return "universesBeyondRoom";//loads universesBeyondRoom.html
            default:
                return "error"; // fallback view
        }
    }

    @PostMapping("/room/{roomName}/join") //Join a room, call comes from joinRoom in  JavaScript(client) and delegates to RoomManager
    @ResponseBody
    public ResponseEntity<?> joinRoom(@PathVariable String roomName, @RequestParam String sessionId) {
        boolean success = roomManager.tryJoin(roomName, sessionId);
        int count = roomManager.userCount(roomName);
        messaging.convertAndSend("/topic/" + roomName + "/count", count); //Notify all subscribers of the new user count
        return success ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).body("Room full"); 
    }

    @PostMapping("/room/{roomName}/leave") //Leave a room, call comes from leaveRoom in JavaScript(client) and delegates to RoomManager
    @ResponseBody
    public ResponseEntity<?> leaveRoom(@PathVariable String roomName, @RequestParam String sessionId) {
        roomManager.leave(roomName, sessionId);
        int count = roomManager.userCount(roomName);
        messaging.convertAndSend("/topic/" + roomName + "/count", count); //Notify all subscribers of the new user count
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/room/{roomName}/count") //Get the user count for a room, call comes from javaScript(client)
    @ResponseBody
    public int getRoomUserCount(@PathVariable String roomName) {
        return roomManager.userCount(roomName);
    }
}