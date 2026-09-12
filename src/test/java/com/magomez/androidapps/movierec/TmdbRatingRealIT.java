package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.IdentificationStatus;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieEnrichment;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.tmdb.TmdbClient;
import com.magomez.androidapps.movierec.provider.tmdb.TmdbMovieDataProvider;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real end-to-end check of the rating-enrichment flow against the live TMDB API:
 * identification &rarr; {@link Movie} &rarr; {@code MovieEnricher.enrich()} &rarr;
 * enriched {@link Movie}.
 *
 * <p>This is NOT a unit test and is NOT executed by {@code mvn test} / {@code mvn
 * package}: the class name ends in {@code IT} (outside Surefire's default include
 * patterns) and it is additionally gated behind the {@code tmdb.integration} system
 * property, so the normal build never depends on Internet.
 *
 * <p>Run it explicitly:
 * <pre>mvn test -Dtest=TmdbRatingRealIT -Dtmdb.integration=true -DfailIfNoTests=false</pre>
 *
 * <p>It exercises the real {@link TmdbMovieDataProvider} and {@link TmdbClient} (no
 * mocks). The API key is read from {@code application.properties}, never hardcoded here.
 */
@EnabledIfSystemProperty(named = "tmdb.integration", matches = "true")
class TmdbRatingRealIT {

    @Test
    void identifiesTheSubstanceAndEnrichesItWithTheRealTmdbRating() throws Exception {
        Properties config = loadApplicationProperties();
        TmdbClient client = new TmdbClient(
                config.getProperty("tmdb.api.key"),
                config.getProperty("tmdb.api.base-url", "https://api.themoviedb.org/3/"));
        TmdbMovieDataProvider provider = new TmdbMovieDataProvider(client);

        // 1. identification
        MovieMatch match = provider.identify(
                new MovieQuery("The Substance", 2024, "Coralie Fargeat"));
        assertThat(match.status()).isEqualTo(IdentificationStatus.IDENTIFIED);

        Movie movie = match.movie();
        assertThat(movie).isNotNull();
        assertThat(movie.tmdbId()).isNotNull();
        assertThat(movie.imdbId()).isNotNull().startsWith("tt");

        // 2. rating enrichment through the generic MovieEnricher contract
        MovieEnrichment enrichment = provider.enrich(movie);
        assertThat(enrichment.status()).isEqualTo(MovieEnrichment.Status.APPLIED);

        Movie enriched = enrichment.movie();
        assertThat(enriched.ratings()).hasSize(1);
        assertThat(movie.ratings()).isEmpty(); // original Movie untouched
        Rating rating = enriched.ratings().get(0);

        assertThat(rating.source()).isEqualTo("TMDB");
        assertThat(rating.score()).isGreaterThan(0.0).isLessThanOrEqualTo(10.0);
        assertThat(rating.voteCount()).isNotNull().isPositive();

        // 3. coherence with the live TMDB payload (no hardcoded expected values)
        TmdbMovieDetails live = client.movieDetails(movie.tmdbId());
        assertThat(rating.score()).isEqualTo(live.voteAverage());
        assertThat(rating.voteCount()).isEqualTo(live.voteCount());
    }

    private static Properties loadApplicationProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream in =
                     TmdbRatingRealIT.class.getResourceAsStream("/application.properties")) {
            assertThat(in).as("application.properties on the test classpath").isNotNull();
            props.load(in);
        }
        return props;
    }
}
