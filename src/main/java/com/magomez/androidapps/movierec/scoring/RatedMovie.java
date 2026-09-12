package com.magomez.androidapps.movierec.scoring;

import com.magomez.androidapps.movierec.model.Movie;

import java.util.Objects;

/**
 * A movie the user has rated. Temporary, immutable input for
 * {@link UserTasteProfileBuilder}; never persisted.
 *
 * @param movie     the rated movie (its own fields are left untouched)
 * @param userScore the user's rating on a <b>0-10</b> scale
 */
public record RatedMovie(Movie movie, double userScore) {

    public RatedMovie {
        Objects.requireNonNull(movie, "movie");
        if (!Double.isFinite(userScore) || userScore < 0.0 || userScore > 10.0) {
            throw new IllegalArgumentException("userScore must be within [0, 10]: " + userScore);
        }
    }
}
