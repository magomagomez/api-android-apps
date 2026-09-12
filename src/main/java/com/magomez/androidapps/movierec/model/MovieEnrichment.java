package com.magomez.androidapps.movierec.model;

/**
 * Outcome of running one enricher over an already-identified {@link Movie}.
 *
 * <p>Temporary and immutable, pure domain. {@link #movie()} is always the movie to
 * carry forward: the enriched copy when {@link Status#APPLIED}, or the untouched input
 * when {@link Status#NO_DATA} or {@link Status#FAILED}. A {@code FAILED} enrichment is
 * deliberately distinct from an identification failure and never demotes the movie.
 *
 * @param movie  the movie to carry on with (never {@code null})
 * @param status what happened
 * @param error  failure detail, present only when {@code status == FAILED}
 */
public record MovieEnrichment(Movie movie, Status status, String error) {

    public enum Status {
        /** Extra data was found and merged into the movie. */
        APPLIED,
        /** The source simply had no data for this movie. */
        NO_DATA,
        /** The lookup could not be completed (source unreachable, bad response...). */
        FAILED
    }

    public static MovieEnrichment applied(Movie movie) {
        return new MovieEnrichment(movie, Status.APPLIED, null);
    }

    public static MovieEnrichment noData(Movie movie) {
        return new MovieEnrichment(movie, Status.NO_DATA, null);
    }

    public static MovieEnrichment failed(Movie movie, String error) {
        return new MovieEnrichment(movie, Status.FAILED, error);
    }

    public boolean isFailure() {
        return status == Status.FAILED;
    }
}
