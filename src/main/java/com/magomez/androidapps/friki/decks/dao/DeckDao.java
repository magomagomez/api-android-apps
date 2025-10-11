package com.magomez.androidapps.friki.decks.dao;

import com.magomez.androidapps.friki.decks.dto.Deck;
import com.magomez.androidapps.friki.decks.dto.DeckFilterRequest;
import com.magomez.androidapps.friki.decks.dto.CreateDeckRequest;
import com.magomez.androidapps.friki.decks.mapper.DeckMapper;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.magomez.androidapps.friki.config.FrikiConfig.AND;
import static com.magomez.androidapps.friki.config.FrikiConfig.FROM;
import static com.magomez.androidapps.friki.config.FrikiConfig.INSERT;
import static com.magomez.androidapps.friki.config.FrikiConfig.SELECT_ALL;
import static com.magomez.androidapps.friki.config.FrikiConfig.VALUES;
import static com.magomez.androidapps.friki.config.FrikiConfig.WHERE;

@Service
public class DeckDao {

    private static final String TABLE_DECKS = " friki_decks ";
    private static final String COLUMN_NAME = " name ";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_WISH = " is_wishList ";
    private static final String COLUMN_CITY = " is_city_deck ";
    private static final String ORDER_BY_NAME = " ORDER BY name ";

    private final JdbcTemplate jdbcTemplate;

    public DeckDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Deck> search(DeckFilterRequest filter) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_DECKS + " ";
        query = query + WHERE +" 1=1 ";
        if( filter.name() != null) {
            query = query + AND + COLUMN_NAME + " = '" + filter.name() + "' ";
        }
        if(filter.wish() != null){
            query = query + AND + COLUMN_WISH + " = " + (BooleanUtils.isTrue(filter.wish()) ? 1:0) ;
        }
        if(filter.city() != null){
            query = query + AND + COLUMN_CITY + " = " + + (BooleanUtils.isTrue(filter.city()) ? 1:0) ;
        }
        query = query + ORDER_BY_NAME;

        return  jdbcTemplate.query( query, new DeckMapper());
    }

    public Deck getDeck(Integer deckId) {

        String query = SELECT_ALL;
        query = query + FROM + TABLE_DECKS + " ";
        query = query + WHERE + COLUMN_ID + "= "+ deckId;
        try {
            return jdbcTemplate.queryForObject(query, new DeckMapper());
        } catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comic not found");
        }
    }

    public void createDeck(CreateDeckRequest deck) {
        String query = INSERT + TABLE_DECKS + " (name,is_city_deck,category,is_wishList) " +
                VALUES + "(?,?,?,?)";
        jdbcTemplate.update(query , deck.name(),
                deck.isCityDeck(), deck.category(), 1);
    }

}
