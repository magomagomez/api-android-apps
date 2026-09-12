package com.magomez.androidapps.movierec.provider;

import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieEnrichment;

/**
 * Adds extra data (ratings today, more later) to an already-identified movie.
 *
 * <p>Generic on purpose: the service orchestrates any number of enrichers without
 * knowing what each one does. Domain in, domain out — no HTTP, DTOs or vendor types.
 *
 * <p>An enricher never throws for a data-source problem: it returns a
 * {@link MovieEnrichment} whose {@code status} distinguishes "applied", "no data" and
 * "failed". A failure here is not an identification failure.
 */
public interface MovieEnricher {

    /**
     * @param movie an identified movie (its id fields are populated)
     * @return the movie to carry forward plus the outcome; never {@code null}
     */
    MovieEnrichment enrich(Movie movie);

    /** Short name for diagnostics, e.g. {@code "TMDB rating"}. */
    String name();
}
