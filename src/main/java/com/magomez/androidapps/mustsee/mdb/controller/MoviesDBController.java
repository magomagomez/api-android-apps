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
@RequestMapping(value = ApiConfig.BASE_URL+"/movieDB/films")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class MoviesDBController {

    private final MovieDBService movieDataBaseService;

    @Autowired
    public MoviesDBController(MovieDBService movieDataBaseService){
        this.movieDataBaseService = movieDataBaseService;
    }

    @GetMapping(value ="/{filmId}")
    public MovieDBDetailsDTO getMovieDBDetails(@PathVariable Integer filmId,
                                               @RequestParam(value="film_type") Integer filmType) throws Exception {
        return movieDataBaseService.getMovieDetails(filmId,filmType);
    }

    @GetMapping
    public List<MovieDBDetailsDTO> searchMoviesDB(@RequestParam(value = "search") String search) throws IOException {
        return movieDataBaseService.searchMovies(search);
    }


}
