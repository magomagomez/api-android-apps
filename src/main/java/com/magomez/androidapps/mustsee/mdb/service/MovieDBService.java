package com.magomez.androidapps.mustsee.mdb.service;

import com.magomez.androidapps.mustsee.mdb.converter.MovieDBConverter;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsDTO;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsList;
import com.magomez.androidapps.mustsee.mdb.repository.MovieDBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MovieDBService {

    private final MovieDBRepository movieDataBaseRepository;

    @Autowired
    public MovieDBService(MovieDBRepository movieDataBaseRepository){
        this.movieDataBaseRepository = movieDataBaseRepository;
    }

    public MovieDBDetailsDTO getMovieDetails(Integer id) throws IOException {
        MovieDBDetails movieDetails = movieDataBaseRepository.getMovieInfo(id);
        return MovieDBConverter.toDto(movieDetails);
    }

    public List<MovieDBDetailsDTO> getMovies(String search) throws IOException {
        MovieDBDetailsList movieDetailsList = movieDataBaseRepository.getMovies(search);
        return MovieDBConverter.toDto(movieDetailsList);
    }

    public MovieDBDetailsDTO getTvShowDetails(Integer id) throws IOException {
        MovieDBDetails movieDetails = movieDataBaseRepository.getTvShowInfo(id);
        return MovieDBConverter.toDto(movieDetails);
    }

    public List<MovieDBDetailsDTO> getTvShows(String search) throws IOException {
        MovieDBDetailsList movieDetailsList = movieDataBaseRepository.getTvShows(search);
        return MovieDBConverter.toDto(movieDetailsList);
    }

}
