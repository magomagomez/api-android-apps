package com.magomez.androidapps.friki.comics.dao;

import com.magomez.androidapps.friki.comics.dto.CreateComicRequest;
import com.magomez.androidapps.friki.comics.dto.Comic;
import com.magomez.androidapps.friki.comics.dto.ComicFilterRequest;
import com.magomez.androidapps.friki.comics.mapper.ComicMapper;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
    private static final String ORDER_BY_NAME = " ORDER BY name ";

    private final JdbcTemplate jdbcTemplate;

    public ComicDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Comic> search(ComicFilterRequest filter) {
        List<Object> args = new ArrayList<>();
        String query = SELECT_ALL + FROM + TABLE_COMICS + " " + WHERE +" 1=1 ";
        if( filter.name() != null) {
            query = query + AND + COLUMN_NAME + " = ? ";
            args.add(filter.name());
        }
        if(filter.wish() != null){
            query = query + AND + COLUMN_WISH + " = ?" ;
            args.add(BooleanUtils.isTrue(filter.wish()) ? 1:0);
        }
        if(filter.marvel() != null){
            query = query + AND + COLUMN_MARVEL + " = ?" ;
            args.add(BooleanUtils.isTrue(filter.marvel()) ? 1:0);
        }
        query = query + ORDER_BY_NAME;

        return jdbcTemplate.query(query, new ComicMapper(), args.toArray());
    }

    public Comic getComic(Integer comicId) {
        String query = SELECT_ALL + FROM + TABLE_COMICS + " " + WHERE + COLUMN_ID + "= ?";
        try {
            return jdbcTemplate.queryForObject(query, new ComicMapper(), comicId);
        } catch(EmptyResultDataAccessException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comic not found");
        }
    }

    public void createComic(CreateComicRequest comic) {
        String query = INSERT + TABLE_COMICS + " (name,is_marvel_or_dc,category,is_wishList) " +
                VALUES + "(?,?,?,?)";
        jdbcTemplate.update(query , comic.name(),
                comic.marvelOrDc(), comic.category(), 1);
    }

}
