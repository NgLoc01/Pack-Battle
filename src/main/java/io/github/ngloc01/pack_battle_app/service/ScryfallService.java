package io.github.ngloc01.pack_battle_app.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import  io.github.ngloc01.pack_battle_app.dataTransferObject.ScryfallCard;

@Service
public class ScryfallService {
    private final RestTemplate restTemplate = new RestTemplate();

    public ScryfallCard fetchRandomCard(String set) {
        return restTemplate.getForObject("https://api.scryfall.com/cards/random?q=set:" + set, ScryfallCard.class);
    }
}

//https://api.scryfall.com/cards/random

//Assassin creed: https://api.scryfall.com/cards/random?q=set:acr
//Dr Who: https://api.scryfall.com/cards/random?q=set:who
//LOTR: https://api.scryfall.com/cards/random?q=set:ltr
//Final fantasy: https://api.scryfall.com/cards/random?q=set:fin

//Modern horizen 3: https://api.scryfall.com/cards/random?q=set:mh3
//Foundation: https://api.scryfall.com/cards/random?q=set:fdn
//Duskmourn: https://api.scryfall.com/cards/random?q=set:dsk
//Bloomburrow: https://api.scryfall.com/cards/random?q=set:blb
