package com.magomez.androidapps.movierec.scoring.letterboxd;

import java.util.Objects;

/**
 * One row of a Letterboxd ratings / diary CSV export. Immutable, temporary; no movie
 * identification has happened yet.
 *
 * @param title         the film title, preserved exactly as it appears in the CSV
 * @param year          the release year, or {@code null} when the CSV column was empty
 * @param letterboxdUri the Letterboxd URI, preserved exactly as it appears, or
 *                      {@code null} when the CSV column was empty
 * @param userScore     the Letterboxd rating (0.5-5.0) converted to the 0-10 scale
 *                      ({@code rating * 2}), so it is within {@code [1, 10]}
 */
public record LetterboxdRatedMovie(String title, Integer year, String letterboxdUri, double userScore) {

    public LetterboxdRatedMovie {
        Objects.requireNonNull(title, "title");
        if (title.isEmpty()) {
            throw new IllegalArgumentException("title must not be empty");
        }
        if (!Double.isFinite(userScore) || userScore < 1.0 || userScore > 10.0) {
            throw new IllegalArgumentException("userScore must be within [1, 10]: " + userScore);
        }
    }
}
