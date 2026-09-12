package com.magomez.androidapps.movierec.controller;

import com.magomez.androidapps.movierec.api.RecommendationApiMapper;
import com.magomez.androidapps.movierec.api.RecommendationRequest;
import com.magomez.androidapps.movierec.api.RecommendationResponse;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.recommendation.RecommendationResult;
import com.magomez.androidapps.movierec.recommendation.RecommendationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Entry point of the RECOMMENDATIONS pipeline: receives a JSON list of candidate movies
 * and returns them ranked by PERSONAL MATCH SCORE against the user's Letterboxd taste.
 *
 * <p>Controller &rarr; Service &rarr; Provider. The controller only adapts between the
 * HTTP contract and the domain (via {@link RecommendationApiMapper}); it holds no
 * business logic and never talks to an external API. The {@code /api/movies/import}
 * contract is untouched.
 */
@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public RecommendationResponse recommend(@RequestBody RecommendationRequest request) {
        List<MovieQuery> candidates = RecommendationApiMapper.toQueries(request);
        RecommendationResult result = recommendationService.recommend(candidates);
        return RecommendationApiMapper.toResponse(result);
    }
}
