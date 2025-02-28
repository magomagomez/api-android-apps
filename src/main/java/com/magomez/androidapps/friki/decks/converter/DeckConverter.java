package com.magomez.androidapps.friki.decks.converter;

import com.magomez.androidapps.friki.decks.dto.Deck;
import com.magomez.androidapps.friki.decks.dto.DeckDTO;

import java.util.List;

public class DeckConverter {

    private DeckConverter(){}

    public static List<DeckDTO> toDtoList(List<Deck> decks){
        return decks.stream()
                .map(DeckConverter::toDto)
                .toList();
    }

    public static DeckDTO toDto(Deck deck) {
        return new DeckDTO(
                deck.id(),
                deck.name(),
                deck.isWishList(),
                deck.categories()
        );
    }

}
