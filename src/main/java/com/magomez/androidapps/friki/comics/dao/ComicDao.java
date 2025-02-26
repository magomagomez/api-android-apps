package com.magomez.androidapps.friki.comics.dao;

import com.magomez.androidapps.friki.comics.dto.CreateComicRequest;
import com.magomez.androidapps.friki.comics.dto.Comic;
import com.magomez.androidapps.friki.comics.dto.ComicFilterRequest;
import com.magomez.androidapps.friki.comics.mapper.ComicMapper;
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
public class ComicDao {

    private static final String TABLE_COMICS = " friki_comics ";
    private static final String COLUMN_NAME = " name ";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_WISH = " is_wishList ";
    private static final String COLUMN_MARVEL = " is_marvel_or_dc ";

    private final JdbcTemplate jdbcTemplate;

    public ComicDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Comic> search(ComicFilterRequest filter) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_COMICS + " ";
        query = query + WHERE +" 1=1 ";
        if( filter.name() != null) {
            query = query + AND + COLUMN_NAME + " = '" + filter.name() + "' ";
        }
        if(BooleanUtils.isTrue(filter.wish())){
            query = query + AND + COLUMN_WISH + " = 1";
        }
        if(BooleanUtils.isTrue(filter.marvel())){
            query = query + AND + COLUMN_MARVEL + " = 1";
        }

        return  jdbcTemplate.query( query, new ComicMapper());
    }

    public Comic getComic(Integer comicId) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_COMICS + " ";
        query = query + WHERE + COLUMN_ID + "= "+ comicId;
        try {
            return jdbcTemplate.queryForObject(query, new ComicMapper());
        } catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comic not found");
        }
    }

    public void createComic(CreateComicRequest comic) {
        String query = INSERT + TABLE_COMICS + " (name,is_marvel_or_dc,category,is_wishList) " +
                VALUES + "(?,?,?,?,?,?)";
        jdbcTemplate.update(query , comic.name(),
                comic.marvelOrDc(), comic.category(), 1);
    }

}
