package com.magomez.androidapps.mustsee.mdb.controller;

import com.magomez.androidapps.mustsee.config.ApiConfig;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDetailsDTO;
import com.magomez.androidapps.mustsee.mdb.service.MovieDataBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL+"/movies")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class MoviesDataBaseController {

    private final MovieDataBaseService movieDataBaseService;

    @Autowired
    public MoviesDataBaseController(MovieDataBaseService movieDataBaseService){
        this.movieDataBaseService = movieDataBaseService;
    }

    @GetMapping(value ="{movieId}")
    public MovieDetailsDTO getMovieDetails(@PathVariable Integer movieId) throws IOException {
        return movieDataBaseService.getMovieDetails(movieId);
    }

    @GetMapping
    public List<MovieDetailsDTO> getMovies( @RequestParam(value = "search") String search) throws IOException {
        return movieDataBaseService.getMovies(search);
    }


}
