package com.magomez.androidapps.friki.decks.service;

import com.magomez.androidapps.friki.decks.converter.DeckConverter;
import com.magomez.androidapps.friki.decks.dao.DeckDao;
import com.magomez.androidapps.friki.decks.dto.Deck;
import com.magomez.androidapps.friki.decks.dto.DeckDTO;
import com.magomez.androidapps.friki.decks.dto.DeckFilterRequest;
import com.magomez.androidapps.friki.decks.dto.CreateDeckRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DeckService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final DeckDao deckDao;

    @Autowired
    public DeckService(DeckDao deckDao){
        this.deckDao = deckDao;
    }

    public List<DeckDTO> search(DeckFilterRequest requestFilter){
        List<Deck> monuments =  deckDao.search(requestFilter);
        return DeckConverter.toDtoList(monuments);
    }

    public DeckDTO getDeck(Integer deckId){
        Deck deck =  deckDao.getDeck(deckId);
        return DeckConverter.toDto(deck);
    }

    public void createDeck(CreateDeckRequest request){
        if(request == null || request.name() == null || request.category() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        deckDao.createDeck(request);
    }
}
