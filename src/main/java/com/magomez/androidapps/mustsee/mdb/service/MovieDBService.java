package com.magomez.androidapps.mustsee.mdb.service;

import com.magomez.androidapps.mustsee.mdb.ResourceNotFoundException;
import com.magomez.androidapps.mustsee.mdb.converter.MovieDBConverter;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsDTO;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsList;
import com.magomez.androidapps.mustsee.mdb.repository.MovieDBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MovieDBService {

    private final MovieDBRepository movieDataBaseRepository;

    @Autowired
    public MovieDBService(MovieDBRepository movieDataBaseRepository){
        this.movieDataBaseRepository = movieDataBaseRepository;
    }

    public MovieDBDetailsDTO getMovieDetails(Integer id, Integer filmType) throws Exception {
        MovieDBDetails movieDetails = movieDataBaseRepository.getMovieInfo(id, filmType);
        if (movieDetails.getId() == null){
            movieDetails = movieDataBaseRepository.getTvShowInfo(id);
            if (movieDetails.getId() == null){
                throw new ResourceNotFoundException();
            }
        }
        return MovieDBConverter.toDto(movieDetails);
    }

    public List<MovieDBDetailsDTO> getMovies(String search) throws IOException {
        MovieDBDetailsList movieDetailsList = movieDataBaseRepository.getMovies(search);
        List<MovieDBDetailsDTO> movies =  MovieDBConverter.toDto(movieDetailsList);
        MovieDBDetailsList tvDetailsList = movieDataBaseRepository.getTvShows(search);
        List<MovieDBDetailsDTO> tv =  MovieDBConverter.toDto(tvDetailsList);
        return Stream.concat(movies.stream(),tv.stream()).collect(Collectors.toList());
    }

}
