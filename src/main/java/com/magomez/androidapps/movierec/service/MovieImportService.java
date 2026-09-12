package com.magomez.androidapps.movierec.service;

import com.magomez.androidapps.movierec.model.IdentificationStatus;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieEnrichment;
import com.magomez.androidapps.movierec.model.MovieIdentificationResult;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import com.magomez.androidapps.movierec.provider.MovieDataProvider;
import com.magomez.androidapps.movierec.provider.MovieEnricher;
import com.magomez.androidapps.movierec.provider.MovieProviderException;
import com.magomez.androidapps.movierec.support.ExternalCallExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Identification + enrichment step of the first vertical: domain queries in, domain
 * results out.
 *
 * <p>Per query: identify it with the {@link MovieDataProvider}; when the movie is
 * {@code IDENTIFIED}, run every registered {@link MovieEnricher} over it in order. The
 * service does not know what each enricher does. Failures are isolated per query and
 * enrichment failures are kept separate from identification failures: they never turn
 * an identified movie into {@code NOT_FOUND}.
 *
 * <p>Talks only to domain interfaces and models; it knows nothing about HTTP, JSON,
 * DTOs or the concrete external source. Stateless: nothing is persisted.
 */
@Service
public class MovieImportService {

    private static final Logger log = LoggerFactory.getLogger(MovieImportService.class);

    private final MovieDataProvider movieDataProvider;
    private final List<MovieEnricher> enrichers;
    private final ExternalCallExecutor externalCallExecutor;

    @Autowired
    public MovieImportService(MovieDataProvider movieDataProvider, List<MovieEnricher> enrichers,
                              ExternalCallExecutor externalCallExecutor) {
        this.movieDataProvider = movieDataProvider;
        this.enrichers = List.copyOf(enrichers);
        this.externalCallExecutor = externalCallExecutor;
    }

    /** Test constructor: identifies and enriches queries inline, on the caller thread. */
    public MovieImportService(MovieDataProvider movieDataProvider, List<MovieEnricher> enrichers) {
        this(movieDataProvider, enrichers, ExternalCallExecutor.sequential());
    }

    public List<MovieIdentificationResult> identifyAll(List<MovieQuery> queries) {
        return externalCallExecutor.map(queries, this::process);
    }

    private MovieIdentificationResult process(MovieQuery query) {
        MovieMatch match;
        try {
            match = movieDataProvider.identify(query);
        } catch (MovieProviderException e) {
            // One bad entry must not fail the whole request; report it as not found.
            log.warn("Provider {} failed to identify '{}': {}",
                    movieDataProvider.sourceName(), query.title(), e.getMessage());
            return MovieIdentificationResult.identificationFailed(query, e.getMessage());
        }

        if (match.status() != IdentificationStatus.IDENTIFIED) {
            return MovieIdentificationResult.of(query, match);
        }
        return enrich(query, match.movie());
    }

    private MovieIdentificationResult enrich(MovieQuery query, Movie identifiedMovie) {
        Movie movie = identifiedMovie;
        List<String> errors = new ArrayList<>();

        for (MovieEnricher enricher : enrichers) {
            MovieEnrichment enrichment = enricher.enrich(movie);
            movie = enrichment.movie();
            if (enrichment.isFailure()) {
                log.warn("Enricher {} failed for '{}': {}",
                        enricher.name(), query.title(), enrichment.error());
                errors.add(enrichment.error());
            }
        }

        MovieMatch enrichedMatch = MovieMatch.identified(movie);
        if (errors.isEmpty()) {
            return MovieIdentificationResult.of(query, enrichedMatch);
        }
        return MovieIdentificationResult.enrichmentFailed(query, enrichedMatch, String.join("; ", errors));
    }
}
