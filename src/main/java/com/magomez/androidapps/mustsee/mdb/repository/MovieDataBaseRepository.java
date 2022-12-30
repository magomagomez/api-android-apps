package com.magomez.androidapps.mustsee.mdb.repository;

import com.magomez.androidapps.mustsee.mdb.datasource.MovieDataBaseDatasource;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetails;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetailsList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MovieDataBaseRepository {

    private final MovieDataBaseDatasource movieDataBaseDatasource;

    @Autowired
    public MovieDataBaseRepository(MovieDataBaseDatasource movieDataBaseDatasource){
        this.movieDataBaseDatasource = movieDataBaseDatasource;
    }

    public MovieDetails getMovieInfo(Integer id) throws IOException {
        return movieDataBaseDatasource.getMovieDetails(id);
    }

    public MovieDetailsList getMovies(String search) throws IOException {
        return movieDataBaseDatasource.getMovies(search);
    }

}
