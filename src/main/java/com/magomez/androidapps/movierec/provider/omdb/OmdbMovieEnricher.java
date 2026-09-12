package com.magomez.androidapps.movierec.provider.omdb;

import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieEnrichment;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.provider.MovieEnricher;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link MovieEnricher} that adds IMDb / Rotten Tomatoes / Metacritic ratings from OMDb.
 *
 * <p>Looks OMDb up strictly by {@link Movie#imdbId()} — never by title, no scraping.
 * New ratings are appended, keeping whatever the movie already has (e.g. the TMDB
 * rating). Scales are not normalized.
 *
 * <ul>
 *   <li>no {@code imdbId} &rarr; {@link MovieEnrichment.Status#NO_DATA};</li>
 *   <li>OMDb has no usable rating &rarr; {@code NO_DATA};</li>
 *   <li>API / HTTP error &rarr; {@link MovieEnrichment.Status#FAILED} (never throws).</li>
 * </ul>
 */
@Service
public class OmdbMovieEnricher implements MovieEnricher {

    private static final String ENRICHER_NAME = "OMDb ratings";

    private final OmdbClient client;

    public OmdbMovieEnricher(OmdbClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return ENRICHER_NAME;
    }

    @Override
    public MovieEnrichment enrich(Movie movie) {
        if (movie == null || movie.imdbId() == null || movie.imdbId().isBlank()) {
            return MovieEnrichment.noData(movie);
        }
        try {
            OmdbResponse response = client.byImdbId(movie.imdbId());
            if (!response.isSuccess()) {
                return MovieEnrichment.noData(movie);
            }
            List<Rating> found = OmdbRatingMapper.toRatings(response);
            if (found.isEmpty()) {
                return MovieEnrichment.noData(movie);
            }
            List<Rating> merged = new ArrayList<>(movie.ratings());
            merged.addAll(found);
            return MovieEnrichment.applied(movie.withRatings(merged));
        } catch (IOException e) {
            return MovieEnrichment.failed(movie,
                    "OMDb ratings lookup failed for imdbId " + movie.imdbId() + ": " + e.getMessage());
        }
    }
}
