package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.controller.MovieImportController;
import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieIdentificationResult;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.MovieDataProvider;
import com.magomez.androidapps.movierec.provider.MovieProviderException;
import com.magomez.androidapps.movierec.service.MovieImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.function.Function;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint contract test. Uses a hand-written {@link MovieImportService} stub (the
 * service is a concrete class and the CI JVM cannot always instrument it) but the real
 * {@code MovieApiMapper}, and a standalone MockMvc so no Spring context / database is
 * needed. The external API is never called. This is the guard for the exact
 * {@code /api/movies/import} JSON contract.
 */
class MovieImportControllerTest {

    private List<MovieQuery> lastQueries;
    private Function<List<MovieQuery>, List<MovieIdentificationResult>> handler = q -> List.of();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MovieDataProvider unusedProvider = new MovieDataProvider() {
            @Override
            public String sourceName() {
                return "STUB";
            }

            @Override
            public MovieMatch identify(MovieQuery query) throws MovieProviderException {
                throw new MovieProviderException("not used");
            }
        };
        MovieImportService stub = new MovieImportService(unusedProvider, List.of()) {
            @Override
            public List<MovieIdentificationResult> identifyAll(List<MovieQuery> queries) {
                lastQueries = queries;
                return handler.apply(queries);
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new MovieImportController(stub)).build();
    }

    @Test
    void importReturnsTheAggregatedResultAsJson() throws Exception {
        Movie substance = new Movie(1064213, "tt17526714", "La sustancia", "The Substance",
                "2024-09-07", 141, "Puedes tener una version mejor de ti misma...",
                List.of(new Genre(27, "Terror"), new Genre(878, "Ciencia ficcion")),
                new Director(1234, "Coralie Fargeat"),
                List.of(new Actor(1, "Demi Moore"), new Actor(2, "Margaret Qualley")),
                List.of(new Country("FR", "Francia"), new Country("GB", "Reino Unido")),
                "https://image.tmdb.org/t/p/w500/substance.jpg",
                List.of(
                        new Rating("TMDB", 7.13, 6419),
                        new Rating("IMDb", 7.3, 150_234),
                        new Rating("Rotten Tomatoes", 89.0, null),
                        new Rating("Metacritic", 78.0, null)));
        handler = queries -> List.of(
                MovieIdentificationResult.of(queries.get(0), MovieMatch.identified(substance)),
                MovieIdentificationResult.of(queries.get(1), MovieMatch.notFound()),
                MovieIdentificationResult.of(queries.get(2),
                        MovieMatch.ambiguous(List.of("Crash (1996)", "Crash (2004)"))));

        String body = """
                {
                  "movies": [
                    {"title": "The Substance", "year": 2024, "director": "Coralie Fargeat"},
                    {"title": "Nope xyz"},
                    {"title": "Crash"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/movies/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.identified").value(1))
                .andExpect(jsonPath("$.notFound").value(1))
                .andExpect(jsonPath("$.ambiguous").value(1))
                .andExpect(jsonPath("$.movies[0].status").value("IDENTIFIED"))
                .andExpect(jsonPath("$.movies[0].query").value("The Substance"))
                .andExpect(jsonPath("$.movies[0].year").value(2024))
                .andExpect(jsonPath("$.movies[0].director").value("Coralie Fargeat"))
                .andExpect(jsonPath("$.movies[0].note").value(nullValue()))
                .andExpect(jsonPath("$.movies[0].movie.tmdbId").value(1064213))
                .andExpect(jsonPath("$.movies[0].movie.imdbId").value("tt17526714"))
                .andExpect(jsonPath("$.movies[0].movie.originalTitle").value("The Substance"))
                // domain value objects are flattened to the plain-string contract
                .andExpect(jsonPath("$.movies[0].movie.director").value("Coralie Fargeat"))
                .andExpect(jsonPath("$.movies[0].movie.genres[0]").value("Terror"))
                .andExpect(jsonPath("$.movies[0].movie.genres[1]").value("Ciencia ficcion"))
                .andExpect(jsonPath("$.movies[0].movie.actors[0]").value("Demi Moore"))
                .andExpect(jsonPath("$.movies[0].movie.countries[1]").value("Reino Unido"))
                // ratings exposed as-is, in Movie.ratings order, voteCount null when unknown
                .andExpect(jsonPath("$.movies[0].movie.ratings.length()").value(4))
                .andExpect(jsonPath("$.movies[0].movie.ratings[0].source").value("TMDB"))
                .andExpect(jsonPath("$.movies[0].movie.ratings[0].score").value(7.13))
                .andExpect(jsonPath("$.movies[0].movie.ratings[0].voteCount").value(6419))
                .andExpect(jsonPath("$.movies[0].movie.ratings[1].source").value("IMDb"))
                .andExpect(jsonPath("$.movies[0].movie.ratings[1].voteCount").value(150234))
                .andExpect(jsonPath("$.movies[0].movie.ratings[2].source").value("Rotten Tomatoes"))
                .andExpect(jsonPath("$.movies[0].movie.ratings[2].score").value(89.0))
                .andExpect(jsonPath("$.movies[0].movie.ratings[2].voteCount").value(nullValue()))
                .andExpect(jsonPath("$.movies[0].movie.ratings[3].source").value("Metacritic"))
                .andExpect(jsonPath("$.movies[0].movie.ratings[3].voteCount").value(nullValue()))
                .andExpect(jsonPath("$.movies[1].status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.movies[1].movie").value(nullValue()))
                .andExpect(jsonPath("$.movies[1].note").value("no match found"))
                .andExpect(jsonPath("$.movies[2].status").value("AMBIGUOUS"))
                .andExpect(jsonPath("$.movies[2].candidates[0]").value("Crash (1996)"));

        assertEquals(3, lastQueries.size());
        assertEquals(2024, lastQueries.get(0).year());
        assertEquals("Coralie Fargeat", lastQueries.get(0).director());
    }

    @Test
    void identifiedMovieWithNoRatingsSerializesAnEmptyArray() throws Exception {
        Movie movie = new Movie(1, "tt1", "t", "t", null, null, null,
                List.of(), null, List.of(), List.of(), null, List.of());
        handler = queries -> List.of(
                MovieIdentificationResult.of(queries.get(0), MovieMatch.identified(movie)));

        mockMvc.perform(post("/api/movies/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movies\":[{\"title\":\"t\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies[0].movie.ratings").isArray())
                .andExpect(jsonPath("$.movies[0].movie.ratings.length()").value(0));
    }

    @Test
    void rejectsNonJsonContentType() throws Exception {
        mockMvc.perform(post("/api/movies/import")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("The Matrix;1999"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void missingTitleIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/movies/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movies\":[{\"year\":2020}]}"))
                .andExpect(status().isBadRequest());
    }
}
