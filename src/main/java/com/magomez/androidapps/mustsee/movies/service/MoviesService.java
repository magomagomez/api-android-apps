package com.magomez.androidapps.mustsee.movies.service;

import com.magomez.androidapps.mustsee.movies.converter.MovieConverter;
import com.magomez.androidapps.mustsee.movies.dto.Film;
import com.magomez.androidapps.mustsee.movies.dto.FilmDTO;
import com.magomez.androidapps.mustsee.movies.repository.MoviesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MoviesService {

    private final MoviesRepository moviesRepository;

    @Autowired
    public MoviesService(MoviesRepository moviesRepository){
        this.moviesRepository = moviesRepository;
    }

    public List<FilmDTO> getMovies(Integer userId){
        List<Film> movies = moviesRepository.getMovies(userId);
        return MovieConverter.toDto(movies);
    }

}
