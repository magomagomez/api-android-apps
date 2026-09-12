package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieEnrichment;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.omdb.OmdbClient;
import com.magomez.androidapps.movierec.provider.omdb.OmdbMovieEnricher;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse.OmdbRating;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hand-written {@link OmdbClient} double (concrete class, JVM can't always instrument
 * it). No test hits the real OMDb API.
 */
class OmdbMovieEnricherTest {

    private final FakeOmdbClient client = new FakeOmdbClient();
    private final OmdbMovieEnricher enricher = new OmdbMovieEnricher(client);

    private static Movie movie(String imdbId, List<Rating> ratings) {
        return new Movie(603, imdbId, "t", "t", null, null, null,
                List.of(), null, List.of(), List.of(), null, ratings);
    }

    private static OmdbResponse success(String imdbRating, String imdbVotes, String metascore,
                                        OmdbRating... ratings) {
        return new OmdbResponse("True", null, imdbRating, imdbVotes, metascore, List.of(ratings));
    }

    @Test
    void appliesTheImdbRatingFromOmdb() {
        client.answer = success("7.3", "150,234", "N/A");

        MovieEnrichment enrichment = enricher.enrich(movie("tt0000001", List.of()));

        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.APPLIED);
        assertThat(enrichment.movie().ratings()).containsExactly(new Rating("IMDb", 7.3, 150_234));
    }

    @Test
    void appliesRottenTomatoesWhenPresent() {
        client.answer = success("N/A", "N/A", "N/A", new OmdbRating("Rotten Tomatoes", "91%"));

        MovieEnrichment enrichment = enricher.enrich(movie("tt0000001", List.of()));

        assertThat(enrichment.movie().ratings())
                .containsExactly(new Rating("Rotten Tomatoes", 91.0, null));
    }

    @Test
    void appliesMetacriticWhenPresent() {
        client.answer = success("N/A", "N/A", "64");

        MovieEnrichment enrichment = enricher.enrich(movie("tt0000001", List.of()));

        assertThat(enrichment.movie().ratings())
                .containsExactly(new Rating("Metacritic", 64.0, null));
    }

    @Test
    void returnsNoDataWhenOmdbHasNoUsableRatings() {
        client.answer = success("N/A", "N/A", "N/A");
        Movie input = movie("tt0000001", List.of());

        MovieEnrichment enrichment = enricher.enrich(input);

        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.NO_DATA);
        assertThat(enrichment.movie()).isSameAs(input);
    }

    @Test
    void returnsNoDataWhenOmdbCannotResolveTheId() {
        client.answer = new OmdbResponse("False", "Incorrect IMDb ID.", null, null, null, null);
        Movie input = movie("tt0000001", List.of());

        assertThat(enricher.enrich(input).status()).isEqualTo(MovieEnrichment.Status.NO_DATA);
    }

    @Test
    void returnsNoDataWhenTheMovieHasNoImdbId() {
        Movie input = movie(null, List.of());

        MovieEnrichment enrichment = enricher.enrich(input);

        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.NO_DATA);
        assertThat(enrichment.movie()).isSameAs(input);
        assertThat(client.called).isFalse();
    }

    @Test
    void returnsNoDataWhenTheImdbIdIsBlank() {
        assertThat(enricher.enrich(movie("   ", List.of())).status())
                .isEqualTo(MovieEnrichment.Status.NO_DATA);
        assertThat(client.called).isFalse();
    }

    @Test
    void returnsFailedWithoutThrowingWhenOmdbErrors() {
        client.error = new IOException("HTTP 500");
        Movie input = movie("tt0000001", List.of());

        MovieEnrichment enrichment = enricher.enrich(input);

        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.FAILED);
        assertThat(enrichment.movie()).isSameAs(input);
        assertThat(enrichment.error()).contains("tt0000001").contains("HTTP 500");
    }

    @Test
    void appendsOmdbRatingsWithoutRemovingTheExistingTmdbRating() {
        client.answer = success("7.3", "150,234", "78",
                new OmdbRating("Rotten Tomatoes", "89%"),
                new OmdbRating("Metacritic", "78/100"));
        Rating tmdb = new Rating("TMDB", 7.13, 6419);

        MovieEnrichment enrichment = enricher.enrich(movie("tt0000001", List.of(tmdb)));

        assertThat(enrichment.movie().ratings()).containsExactly(
                tmdb,
                new Rating("IMDb", 7.3, 150_234),
                new Rating("Rotten Tomatoes", 89.0, null),
                new Rating("Metacritic", 78.0, null));
    }

    private static final class FakeOmdbClient extends OmdbClient {

        private OmdbResponse answer;
        private IOException error;
        private boolean called;

        private FakeOmdbClient() {
            super("test-key", "http://localhost/");
        }

        @Override
        public OmdbResponse byImdbId(String imdbId) throws IOException {
            called = true;
            if (error != null) {
                throw error;
            }
            return answer;
        }
    }
}
