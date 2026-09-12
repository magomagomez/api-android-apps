package com.magomez.androidapps.movierec.scoring;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Deterministic QUALITY SCORE of a movie, on the common 0-100 scale.
 *
 * <p>Output of {@link QualityScoreCalculator}: the simple arithmetic mean of the
 * movie's ratings after normalization ({@link RatingNormalizer}), rounded to one
 * decimal. No source weighting yet, no LLM, no PERSONAL MATCH SCORE, no ranking.
 *
 * <p>A movie with no ratings has no score: {@link #value()} is
 * {@link OptionalDouble#empty()}.
 *
 * <p>{@link #normalizedRatings()} keeps the normalized ratings the score was built
 * from (each carrying its untouched original), so the result stays explainable.
 *
 * @param value            the QUALITY SCORE in {@code [0, 100]}, or empty when there
 *                         is no rating data
 * @param normalizedRatings the normalized ratings the mean was computed from
 */
public record QualityScore(OptionalDouble value, List<NormalizedRating> normalizedRatings) {

    public QualityScore {
        Objects.requireNonNull(value, "value");
        if (value.isPresent()) {
            double v = value.getAsDouble();
            if (!Double.isFinite(v) || v < 0.0 || v > 100.0) {
                throw new IllegalArgumentException("QUALITY SCORE must be within [0, 100]: " + v);
            }
        }
        normalizedRatings = normalizedRatings == null ? List.of() : List.copyOf(normalizedRatings);
    }

    public static QualityScore noData() {
        return new QualityScore(OptionalDouble.empty(), List.of());
    }

    public static QualityScore of(double value, List<NormalizedRating> normalizedRatings) {
        return new QualityScore(OptionalDouble.of(value), normalizedRatings);
    }

    public boolean hasValue() {
        return value.isPresent();
    }
}
