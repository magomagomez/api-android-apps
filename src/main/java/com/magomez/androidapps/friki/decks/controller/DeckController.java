package com.magomez.androidapps.friki.decks.controller;
import com.magomez.androidapps.friki.config.ApiConfig;
import com.magomez.androidapps.friki.decks.dto.DeckDTO;
import com.magomez.androidapps.friki.decks.dto.DeckFilterRequest;
import com.magomez.androidapps.friki.decks.dto.CreateDeckRequest;
import com.magomez.androidapps.friki.decks.service.DeckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL + "/decks")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class DeckController {

    private final DeckService deckService;

    @Autowired
    public DeckController(DeckService deckService){
        this.deckService = deckService;
    }

    @GetMapping
    public List<DeckDTO> search(@Valid DeckFilterRequest requestFilter) {

        return deckService.search(requestFilter);
    }

    @GetMapping("{deckId}")
    public DeckDTO getDeck(@PathVariable Integer deckId) {

        return deckService.getDeck(deckId);
    }

    @PostMapping
    public void createDeck(@RequestBody CreateDeckRequest request) {
        deckService.createDeck(request);
    }

}
