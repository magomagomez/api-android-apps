package com.magomez.androidapps.movierec.model;

import java.util.List;

/**
 * Outcome of asking a {@code MovieDataProvider} to identify a {@link MovieQuery}.
 *
 * @param status     how the identification ended
 * @param movie      the identified movie, present only when {@code status == IDENTIFIED}
 * @param candidates human readable descriptions of the competing matches, present only
 *                   when {@code status == AMBIGUOUS}
 */
public record MovieMatch(IdentificationStatus status, Movie movie, List<String> candidates) {

    public MovieMatch {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public static MovieMatch identified(Movie movie) {
        return new MovieMatch(IdentificationStatus.IDENTIFIED, movie, List.of());
    }

    public static MovieMatch notFound() {
        return new MovieMatch(IdentificationStatus.NOT_FOUND, null, List.of());
    }

    public static MovieMatch ambiguous(List<String> candidates) {
        return new MovieMatch(IdentificationStatus.AMBIGUOUS, null, candidates);
    }
}
