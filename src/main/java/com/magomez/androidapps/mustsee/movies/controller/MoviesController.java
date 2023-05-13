package com.magomez.androidapps.mustsee.movies.controller;

import com.magomez.androidapps.mustsee.config.ApiConfig;
import com.magomez.androidapps.mustsee.movies.dto.FilmDTO;
import com.magomez.androidapps.mustsee.movies.service.MoviesService;
import com.magomez.androidapps.mustsee.users.dto.FilmRecomendationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL+"/users/{userId}/movies")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class MoviesController {

    private final MoviesService moviesService;

    @Autowired
    public MoviesController(MoviesService moviesService){
        this.moviesService = moviesService;
    }

    @GetMapping
    public List<FilmDTO> getMovies(@PathVariable(value = "userId") Integer userId){
        return moviesService.getMovies(userId);
    }

    @PostMapping(value="/{movieId}")
    public void insertRecomendation(@PathVariable(value = "userId") Integer userId,
                                    @PathVariable(value = "movieId") Integer movieId,
                                    @RequestBody FilmRecomendationRequest film) throws Exception {

        moviesService.insertRecomendation(userId,movieId,film);
    }

    @PutMapping(value="/{movieId}")
    public void markFilmAsSeen(@PathVariable(value = "userId") Integer userId,
                                    @PathVariable(value = "movieId") Integer movieId){

        moviesService.markFilmAsSeen(userId,movieId);
    }


}
