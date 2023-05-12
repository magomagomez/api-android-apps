package com.magomez.androidapps.mustsee.movies.repository;

import com.magomez.androidapps.mustsee.movies.dao.FilmsDao;
import com.magomez.androidapps.mustsee.movies.dto.Film;
import com.magomez.androidapps.mustsee.users.dto.FilmRecomendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
;
import java.util.List;

@Service
public class MoviesRepository {

    private static final Integer MOVIE_TYPE = 1;

    private final FilmsDao moviesDao;

    @Autowired
    public MoviesRepository(FilmsDao moviesDao){
        this.moviesDao = moviesDao;
    }

    public List<Film> getMovies(Integer userId){
        return moviesDao.getFilms(userId, MOVIE_TYPE);
    }

    public void insertRecomendation(FilmRecomendation film){
        moviesDao.insertRecomendation(film);
    }

}
