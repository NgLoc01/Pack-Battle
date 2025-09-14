package io.github.ngloc01.pack_battle_app.controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.DestinationVariable;

import io.github.ngloc01.pack_battle_app.model.Money; 


@Controller
public class MoneyController {
    
    private final SimpMessagingTemplate messaging;

    public MoneyController(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @MessageMapping("/{roomName}/update-money")
    public void updateMoney(@DestinationVariable String roomName, @Payload Money moneyUpdate, @Header("simpSessionId") String simpSessionId) {
        moneyUpdate.setSessionId(simpSessionId);
        messaging.convertAndSend("/topic/" + roomName + "/money", moneyUpdate);
    }
}