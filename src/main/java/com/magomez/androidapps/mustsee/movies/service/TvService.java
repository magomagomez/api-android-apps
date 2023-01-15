package com.magomez.androidapps.mustsee.movies.service;

import com.magomez.androidapps.mustsee.movies.converter.MovieConverter;
import com.magomez.androidapps.mustsee.movies.dto.Film;
import com.magomez.androidapps.mustsee.movies.dto.FilmDTO;
import com.magomez.androidapps.mustsee.movies.repository.TvRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TvService {

    private final TvRepository tvRepository;

    @Autowired
    public TvService(TvRepository tvRepository){
        this.tvRepository = tvRepository;
    }

    public List<FilmDTO> getTvShows(Integer userId){
        List<Film> tvShows = tvRepository.getTvShow(userId);
        return MovieConverter.toDto(tvShows);
    }

}
