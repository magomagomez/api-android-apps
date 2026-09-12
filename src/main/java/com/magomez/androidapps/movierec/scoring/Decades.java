package com.magomez.androidapps.movierec.scoring;

import java.util.OptionalInt;

/**
 * The single, deterministic rule for deriving a movie's decade from its release date.
 * Shared by {@link PersonalAffinityCalculator} and {@link UserTasteProfileBuilder} so
 * they always agree.
 *
 * <p>A decade is represented by its first year (e.g. {@code 2020} for 2020-2029).
 */
final class Decades {

    private static final int MIN_YEAR = 1870;
    private static final int MAX_YEAR = 2100;

    private Decades() {
    }

    /**
     * Decade (first year) of {@code releaseDate} (expected to start with {@code "YYYY"}),
     * or empty when the year is missing / out of the {@code [1870, 2100]} range.
     */
    static OptionalInt of(String releaseDate) {
        if (releaseDate == null) {
            return OptionalInt.empty();
        }
        String trimmed = releaseDate.trim();
        if (trimmed.length() < 4) {
            return OptionalInt.empty();
        }
        try {
            int year = Integer.parseInt(trimmed.substring(0, 4));
            if (year < MIN_YEAR || year > MAX_YEAR) {
                return OptionalInt.empty();
            }
            return OptionalInt.of((year / 10) * 10);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
