package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieEnrichment;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.omdb.OmdbClient;
import com.magomez.androidapps.movierec.provider.omdb.OmdbMovieEnricher;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Real end-to-end check of the OMDb ratings enricher against the live OMDb API, same
 * arrangement as {@code TmdbRatingRealIT}.
 *
 * <p>NOT a unit test and NOT run by {@code mvn test} / {@code mvn package}: the class
 * name ends in {@code IT} (outside Surefire's default include patterns) and it is
 * gated behind the {@code omdb.integration} system property. It also skips itself when
 * {@code omdb.api.key} is not configured yet.
 *
 * <p>Run it explicitly (after setting {@code omdb.api.key} in application.properties):
 * <pre>mvn test -Dtest=OmdbRatingsRealIT -Domdb.integration=true -DfailIfNoTests=false</pre>
 *
 * <p>Uses the real {@link OmdbMovieEnricher} / {@link OmdbClient}. The API key is read
 * from {@code application.properties}, never hardcoded here.
 */
@EnabledIfSystemProperty(named = "omdb.integration", matches = "true")
class OmdbRatingsRealIT {

    // "The Substance" (2024) — a stable, well-rated IMDb id.
    private static final String THE_SUBSTANCE_IMDB_ID = "tt17526714";

    @Test
    void enrichesAMovieWithRealOmdbRatingsKeepingTheExistingTmdbRating() throws Exception {
        Properties config = loadApplicationProperties();
        String apiKey = config.getProperty("omdb.api.key", "");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "omdb.api.key not configured");

        OmdbClient client = new OmdbClient(
                apiKey, config.getProperty("omdb.api.base-url", "https://www.omdbapi.com/"));
        OmdbMovieEnricher enricher = new OmdbMovieEnricher(client);

        Rating tmdb = Rating.of("TMDB", 7.13);
        Movie movie = new Movie(null, THE_SUBSTANCE_IMDB_ID, "The Substance", "The Substance",
                "2024-09-07", null, null, List.of(), null, List.of(), List.of(), null,
                List.of(tmdb));

        MovieEnrichment enrichment = enricher.enrich(movie);
        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.APPLIED);

        Movie enriched = enrichment.movie();
        assertThat(enriched.ratings()).contains(tmdb); // TMDB preserved
        assertThat(enriched.ratings()).extracting(Rating::source).contains("IMDb");

        Rating imdb = enriched.ratings().stream()
                .filter(r -> r.source().equals("IMDb")).findFirst().orElseThrow();
        assertThat(imdb.score()).isGreaterThan(0.0).isLessThanOrEqualTo(10.0);

        // coherence with the live OMDb payload (no hardcoded expected values)
        OmdbResponse live = client.byImdbId(THE_SUBSTANCE_IMDB_ID);
        assertThat(imdb.score()).isEqualTo(Double.parseDouble(live.imdbRating()));
    }

    private static Properties loadApplicationProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream in =
                     OmdbRatingsRealIT.class.getResourceAsStream("/application.properties")) {
            assertThat(in).as("application.properties on the test classpath").isNotNull();
            props.load(in);
        }
        return props;
    }
}
