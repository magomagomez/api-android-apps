package com.magomez.androidapps.mustsee.movies.controller;

import com.magomez.androidapps.mustsee.config.ApiConfig;
import com.magomez.androidapps.mustsee.movies.dto.FilmDTO;
import com.magomez.androidapps.mustsee.movies.service.MoviesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL+"/movies")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class MoviesController {

    private final MoviesService moviesService;

    @Autowired
    public MoviesController(MoviesService moviesService){
        this.moviesService = moviesService;
    }

    @GetMapping
    public List<FilmDTO> getMovies(@RequestParam(value = "user_id") Integer userId){
        return moviesService.getMovies(userId);
    }


}
