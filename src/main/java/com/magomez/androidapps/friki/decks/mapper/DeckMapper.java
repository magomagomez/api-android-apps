package com.magomez.androidapps.friki.decks.mapper;

import com.magomez.androidapps.friki.decks.dto.Deck;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class DeckMapper implements RowMapper<Deck> {

    @Override
    public Deck mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Deck(
                rs.getInt("id"),
                rs.getString("name"),
                BooleanUtils.isTrue(rs.getInt("is_wishList") == 1),
                Arrays.asList(rs.getString("category").split(",", -1)));
    }
}
