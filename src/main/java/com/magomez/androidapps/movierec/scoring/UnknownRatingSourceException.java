package com.magomez.androidapps.movierec.scoring;

/**
 * Raised by {@link RatingNormalizer} when a rating's {@code source} has no known scale,
 * so it cannot be normalized. Unknown sources are never normalized silently.
 */
public class UnknownRatingSourceException extends IllegalArgumentException {

    public UnknownRatingSourceException(String source) {
        super("No normalization scale is defined for rating source: " + source);
    }
}
