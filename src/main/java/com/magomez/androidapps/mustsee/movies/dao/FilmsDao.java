package com.magomez.androidapps.mustsee.movies.dao;

import com.magomez.androidapps.mustsee.movies.dto.Film;
import com.magomez.androidapps.mustsee.movies.mapper.MoviesMapper;
import com.magomez.androidapps.mustsee.users.dto.FilmRecomendation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.FROM;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.INSERT;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.SELECT_ALL;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.SET;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.UPDATE;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.VALUES;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.WHERE;

@Service
public class FilmsDao {

    private static final String TABLE_MOVIES = "films_recommend";

    private final JdbcTemplate jdbcTemplate;

    public FilmsDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Film> getFilms(Integer userId, Integer type) {
        String query = SELECT_ALL + FROM + TABLE_MOVIES + " "
                + WHERE + " id_user = ? AND film_type = ? AND done = 0" ;

        return jdbcTemplate.query(query, new MoviesMapper(), userId, type);
    }

    public void insertRecomendation(FilmRecomendation film, Integer userId){
        String query = INSERT + TABLE_MOVIES + "(name,image_url,id_external,platform,id_user,id_friend,film_type) " +
                VALUES + "(?,?,?,?,?,?,?)";
        jdbcTemplate.update(query , film.getTittle(), film.getImageUrl(),film.getId(),film.getPlatform().toString(),
                userId, film.getFriendId(), film.getFilmType());
    }

    public void markFilmAsSeen(Integer userId, Integer movieId){
        String query = UPDATE + TABLE_MOVIES
                + " " + SET +" done = 1" + WHERE +" id_user = ? AND id_external = ?";
        jdbcTemplate.update(query, userId, movieId);
    }

}
