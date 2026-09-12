package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.api.MovieApiMapper;
import com.magomez.androidapps.movierec.api.MovieImportRequest;
import com.magomez.androidapps.movierec.api.MovieImportRequest.Entry;
import com.magomez.androidapps.movierec.api.MovieImportResponse;
import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.IdentificationStatus;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieIdentificationResult;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.model.Rating;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieApiMapperTest {

    // --- toQueries ---------------------------------------------------------------

    @Test
    void toQueriesMapsTitleYearAndDirector() {
        List<MovieQuery> queries = MovieApiMapper.toQueries(new MovieImportRequest(List.of(
                new Entry("The Substance", 2024, "Coralie Fargeat"),
                new Entry("Perfect Days", null, null))));

        assertThat(queries).containsExactly(
                new MovieQuery("The Substance", 2024, "Coralie Fargeat"),
                new MovieQuery("Perfect Days", null, null));
    }

    @Test
    void toQueriesRejectsAnEntryWithoutTitle() {
        MovieImportRequest request = new MovieImportRequest(Arrays.asList(
                new Entry("Fine", null, null),
                new Entry("  ", 2020, null)));

        assertThatThrownBy(() -> MovieApiMapper.toQueries(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("movies[1].title is required");
    }

    @Test
    void toQueriesTreatsNullRequestAsEmpty() {
        assertThat(MovieApiMapper.toQueries(null)).isEmpty();
    }

    // --- toResponse -------------------------------------------------------------

    @Test
    void toResponseAggregatesCountsByStatus() {
        MovieImportResponse response = MovieApiMapper.toResponse(List.of(
                MovieIdentificationResult.of(query("A"), MovieMatch.identified(sampleMovie())),
                MovieIdentificationResult.of(query("B"), MovieMatch.notFound()),
                MovieIdentificationResult.of(query("C"), MovieMatch.ambiguous(List.of("C (1996)", "C (2004)"))),
                MovieIdentificationResult.identificationFailed(query("D"), "TMDB down")));

        assertThat(response.total()).isEqualTo(4);
        assertThat(response.identified()).isEqualTo(1);
        assertThat(response.notFound()).isEqualTo(2); // not-found + failed
        assertThat(response.ambiguous()).isEqualTo(1);
    }

    @Test
    void toResponseFlattensTheDomainMovieToTheStringContract() {
        Movie movie = new Movie(1064213, "tt17526714", "La sustancia", "The Substance",
                "2024-09-07", 141, "overview",
                List.of(new Genre(27, "Terror"), new Genre(878, "Ciencia ficcion")),
                new Director(1234, "Coralie Fargeat"),
                List.of(new Actor(1, "Demi Moore"), new Actor(2, "Margaret Qualley")),
                List.of(new Country("FR", "Francia"), new Country("GB", "Reino Unido")),
                "https://image.tmdb.org/t/p/w500/x.jpg",
                List.of());

        MovieImportResponse.Item item = MovieApiMapper.toResponse(List.of(
                MovieIdentificationResult.of(query("The Substance"), MovieMatch.identified(movie))))
                .movies().get(0);

        assertThat(item.movie().tmdbId()).isEqualTo(1064213);
        assertThat(item.movie().imdbId()).isEqualTo("tt17526714");
        assertThat(item.movie().genres()).containsExactly("Terror", "Ciencia ficcion");
        assertThat(item.movie().director()).isEqualTo("Coralie Fargeat");
        assertThat(item.movie().actors()).containsExactly("Demi Moore", "Margaret Qualley");
        assertThat(item.movie().countries()).containsExactly("Francia", "Reino Unido");
        assertThat(item.movie().poster()).isEqualTo("https://image.tmdb.org/t/p/w500/x.jpg");
        assertThat(item.movie().ratings()).isEmpty();
    }

    @Test
    void toResponseExposesTheMovieRatingsInOrderPreservingUnknownVoteCounts() {
        Rating tmdb = new Rating("TMDB", 7.13, 6419);
        Rating imdb = new Rating("IMDb", 7.3, 150_234);
        Rating rottenTomatoes = new Rating("Rotten Tomatoes", 89.0, null);
        Rating metacritic = new Rating("Metacritic", 78.0, null);
        Movie movie = new Movie(1, "tt1", "t", "t", null, null, null,
                List.of(), null, List.of(), List.of(), null,
                List.of(tmdb, imdb, rottenTomatoes, metacritic));

        MovieImportResponse.Item item = MovieApiMapper.toResponse(List.of(
                MovieIdentificationResult.of(query("t"), MovieMatch.identified(movie))))
                .movies().get(0);

        assertThat(item.movie().ratings()).containsExactly(tmdb, imdb, rottenTomatoes, metacritic);
        assertThat(item.movie().ratings().get(2).voteCount()).isNull();
        assertThat(item.movie().ratings().get(3).voteCount()).isNull();
    }

    @Test
    void toResponseDerivesNotesPerOutcome() {
        MovieImportResponse response = MovieApiMapper.toResponse(List.of(
                MovieIdentificationResult.of(query("A"), MovieMatch.identified(sampleMovie())),
                MovieIdentificationResult.of(query("B"), MovieMatch.notFound()),
                MovieIdentificationResult.of(query("C"), MovieMatch.ambiguous(List.of("C (1996)"))),
                MovieIdentificationResult.identificationFailed(query("D"), "TMDB down")));

        assertThat(response.movies().get(0).note()).isNull();
        assertThat(response.movies().get(1).note()).isEqualTo("no match found");
        assertThat(response.movies().get(2).note()).contains("several plausible matches");
        assertThat(response.movies().get(3).note()).isEqualTo("provider error: TMDB down");
    }

    @Test
    void toResponseEchoesRequestedFieldsAndCandidates() {
        MovieImportResponse.Item item = MovieApiMapper.toResponse(List.of(
                MovieIdentificationResult.of(new MovieQuery("Crash", 2005, "Paul Haggis"),
                        MovieMatch.ambiguous(List.of("Crash (1996)", "Crash (2004)")))))
                .movies().get(0);

        assertThat(item.query()).isEqualTo("Crash");
        assertThat(item.year()).isEqualTo(2005);
        assertThat(item.director()).isEqualTo("Paul Haggis");
        assertThat(item.status()).isEqualTo(IdentificationStatus.AMBIGUOUS);
        assertThat(item.movie()).isNull();
        assertThat(item.candidates()).containsExactly("Crash (1996)", "Crash (2004)");
    }

    private static MovieQuery query(String title) {
        return new MovieQuery(title, null, null);
    }

    private static Movie sampleMovie() {
        return new Movie(1, "tt1", "t", "t", "2000-01-01", 100, "o",
                List.of(), Director.of("d"), List.of(), List.of(), null, List.of());
    }
}
