package io.github.ngloc01.pack_battle_app.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class PackSelectorController {
    private final SimpMessagingTemplate messaging;

    public PackSelectorController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @MessageMapping("/{roomName}/pick-pack") //Call upon from  stompClient.send(`/app/rooms/pick-pack`, {}, packName); in rooms.html
    public void broadcastPickedPack(@DestinationVariable String roomName, @Payload String packName) {
        messaging.convertAndSend("/topic/" + roomName + "/pack-picks", packName); // Broadcast to all clients subscribed to this room in script.js, in script.js disablePackButton(packName); gets called and broadcasts the packName to all clients.
    }

    @MessageMapping("/{roomName}/unpick-pack")
    public void broadcastUnpickedPack(@DestinationVariable String roomName, @Payload String packName) {
        messaging.convertAndSend("/topic/" + roomName + "/pack-unpicks", packName);
    }
}
