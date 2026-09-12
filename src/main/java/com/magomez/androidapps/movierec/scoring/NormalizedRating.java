package com.magomez.androidapps.movierec.scoring;

import com.magomez.androidapps.movierec.model.Rating;

import java.util.Objects;

/**
 * A {@link Rating} expressed on the common 0-100 scale.
 *
 * <p>Output of {@link RatingNormalizer}. The original rating is kept untouched in
 * {@link #original()} (its value and its own scale are preserved); only {@link #score()}
 * is the normalized 0-100 value.
 *
 * <p>This lives in {@code scoring/}, not {@code model/}: normalization is a scoring
 * concern.
 *
 * @param original the untouched source rating
 * @param score    the normalized value, always within {@code [0, 100]}
 */
public record NormalizedRating(Rating original, double score) {

    public NormalizedRating {
        Objects.requireNonNull(original, "original");
        if (!Double.isFinite(score) || score < 0.0 || score > 100.0) {
            throw new IllegalArgumentException(
                    "normalized score must be within [0, 100]: " + score);
        }
    }

    /** Convenience: the source of the {@link #original()} rating. */
    public String source() {
        return original.source();
    }
}
