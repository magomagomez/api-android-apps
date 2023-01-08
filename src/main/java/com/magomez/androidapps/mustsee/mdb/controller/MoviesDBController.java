package com.magomez.androidapps.mustsee.mdb.controller;

import com.magomez.androidapps.mustsee.config.ApiConfig;
import com.magomez.androidapps.mustsee.mdb.dto.MovieDBDetailsDTO;
import com.magomez.androidapps.mustsee.mdb.service.MovieDBService;
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
@RequestMapping(value = ApiConfig.BASE_URL+"/movieDB")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class MoviesDBController {

    private final MovieDBService movieDataBaseService;

    @Autowired
    public MoviesDBController(MovieDBService movieDataBaseService){
        this.movieDataBaseService = movieDataBaseService;
    }

    @GetMapping(value ="/movies/{movieId}")
    public MovieDBDetailsDTO getMovieDetails(@PathVariable Integer movieId) throws IOException {
        return movieDataBaseService.getMovieDetails(movieId);
    }

    @GetMapping(value ="/movies")
    public List<MovieDBDetailsDTO> getMovies(@RequestParam(value = "search") String search) throws IOException {
        return movieDataBaseService.getMovies(search);
    }

    @GetMapping(value ="/tv/{tvId}")
    public MovieDBDetailsDTO getTvShowsDetails(@PathVariable Integer tvId) throws IOException {
        return movieDataBaseService.getTvShowDetails(tvId);
    }

    @GetMapping(value ="/tv")
    public List<MovieDBDetailsDTO> getTvShows(@RequestParam(value = "search") String search) throws IOException {
        return movieDataBaseService.getTvShows(search);
    }


}
