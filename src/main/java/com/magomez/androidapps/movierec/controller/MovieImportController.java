package com.magomez.androidapps.movierec.controller;

import com.magomez.androidapps.movierec.api.MovieApiMapper;
import com.magomez.androidapps.movierec.api.MovieImportRequest;
import com.magomez.androidapps.movierec.api.MovieImportResponse;
import com.magomez.androidapps.movierec.model.MovieIdentificationResult;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.service.MovieImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * First vertical entry point: receives a JSON list of movies and returns the movies
 * identified through the configured provider.
 *
 * <p>Controller &rarr; Service &rarr; Provider &rarr; external API. The controller only
 * adapts between the HTTP contract and the domain (via {@link MovieApiMapper}); it holds
 * no business logic and never talks to the external API.
 */
@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
public class MovieImportController {

    private final MovieImportService movieImportService;

    public MovieImportController(MovieImportService movieImportService) {
        this.movieImportService = movieImportService;
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MovieImportResponse importMovies(@RequestBody MovieImportRequest request) {
        List<MovieQuery> queries = MovieApiMapper.toQueries(request);
        List<MovieIdentificationResult> results = movieImportService.identifyAll(queries);
        return MovieApiMapper.toResponse(results);
    }
}
