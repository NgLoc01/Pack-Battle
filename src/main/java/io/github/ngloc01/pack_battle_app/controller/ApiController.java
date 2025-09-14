package io.github.ngloc01.pack_battle_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.github.ngloc01.pack_battle_app.dataTransferObject.ScryfallCard;
import io.github.ngloc01.pack_battle_app.service.ScryfallService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ScryfallService scryfallService;

    public ApiController(ScryfallService scryfallService) {
        this.scryfallService = scryfallService;
    }

    @GetMapping("/card") //goes to fetchOneCard() in rooms.js
    public ResponseEntity<ScryfallCard> getCard(@RequestParam String mtgSet) { //uses ScryfallCard DTO
        return ResponseEntity.ok(scryfallService.fetchRandomCard(mtgSet)); //uses ScryfallService to fetch a random card
    }
}