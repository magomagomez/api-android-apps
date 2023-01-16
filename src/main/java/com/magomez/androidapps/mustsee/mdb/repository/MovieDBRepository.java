package com.magomez.androidapps.mustsee.mdb.repository;

import com.magomez.androidapps.mustsee.mdb.datasource.MovieDBDatasource;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MovieDBRepository {

    private final MovieDBDatasource movieDataBaseDatasource;

    @Autowired
    public MovieDBRepository(MovieDBDatasource movieDataBaseDatasource){
        this.movieDataBaseDatasource = movieDataBaseDatasource;
    }

    public MovieDBDetails getMovieInfo(Integer id, Integer filmType) throws IOException {
        return movieDataBaseDatasource.getMovieDetails(id,filmType);
    }

    public MovieDBDetailsList getMovies(String search) throws IOException {
        return movieDataBaseDatasource.getMovies(search);
    }

    public MovieDBDetails getTvShowInfo(Integer id) throws IOException {
        return movieDataBaseDatasource.getTvShowsDetails(id);
    }

    public MovieDBDetailsList getTvShows(String search) throws IOException {
        return movieDataBaseDatasource.getTvShows(search);
    }

}
