package com.magomez.androidapps.mustsee.movies.dao;

import com.magomez.androidapps.mustsee.movies.dto.Film;
import com.magomez.androidapps.mustsee.movies.mapper.MoviesMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.FROM;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.SELECT_ALL;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.WHERE;

@Service
public class FilmsDao {

    private static final String TABLE_MOVIES = "films_recommend";

    private final JdbcTemplate jdbcTemplate;

    public FilmsDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Film> getFilms(Integer userId, Integer type) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_MOVIES + " ";
        query = query + WHERE + " id_user =  " + userId + " AND film_type = " + type;

        return  jdbcTemplate.query(query, new MoviesMapper());
    }

}
