package com.magomez.androidapps.mustsee.movies.repository;

import com.magomez.androidapps.mustsee.movies.dao.FilmsDao;
import com.magomez.androidapps.mustsee.movies.dto.Film;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TvRepository {

    private static final Integer TV_SHOW_TYPE = 2;

    private final FilmsDao moviesDao;

    @Autowired
    public TvRepository(FilmsDao moviesDao){
        this.moviesDao = moviesDao;
    }

    public List<Film> getTvShow(Integer userId){
        return moviesDao.getFilms(userId, TV_SHOW_TYPE);
    }

}
