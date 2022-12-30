package com.magomez.androidapps.mustsee.mdb.service;

import com.magomez.androidapps.mustsee.mdb.converter.MovieDataBaseConverter;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetailsDTO;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetailsList;
import com.magomez.androidapps.mustsee.mdb.repository.MovieDataBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MovieDataBaseService {

    private final MovieDataBaseRepository movieDataBaseRepository;

    @Autowired
    public MovieDataBaseService(MovieDataBaseRepository movieDataBaseRepository){
        this.movieDataBaseRepository = movieDataBaseRepository;
    }

    public MovieDetailsDTO getMovieDetails(Integer id) throws IOException {
        MovieDetails movieDetails = movieDataBaseRepository.getMovieInfo(id);
        return MovieDataBaseConverter.toDto(movieDetails);
    }

    public List<MovieDetailsDTO> getMovies(String search) throws IOException {
        MovieDetailsList movieDetailsList = movieDataBaseRepository.getMovies(search);
        return MovieDataBaseConverter.toDto(movieDetailsList);
    }

}
