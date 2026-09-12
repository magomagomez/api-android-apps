package com.magomez.androidapps.movierec.scoring;

import com.magomez.androidapps.movierec.model.Rating;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Pure, stateless normalization of a {@link Rating} to the common 0-100 scale.
 *
 * <p>Lives in {@code scoring/}, not {@code model/}: normalization is a scoring concern.
 * Providers (TMDB, OMDb) keep emitting ratings on their own scale, and
 * {@code Movie.ratings} is never modified.
 *
 * <p>The scale is chosen from {@link Rating#source()}:
 * <ul>
 *   <li>{@code TMDB}, {@code IMDb}: 0-10 &rarr; multiplied by 10;</li>
 *   <li>{@code Rotten Tomatoes}, {@code Metacritic}: already 0-100, unchanged.</li>
 * </ul>
 * An unrecognised source is never normalized silently:
 * {@link #normalize(Rating)} throws {@link UnknownRatingSourceException}.
 *
 * <p>No QUALITY / PERSONAL MATCH score and no ranking here — only the scale change.
 */
@Component
public class RatingNormalizer {

    static final String TMDB = "TMDB";
    static final String IMDB = "IMDb";
    static final String ROTTEN_TOMATOES = "Rotten Tomatoes";
    static final String METACRITIC = "Metacritic";

    /** Full-marks value of each source's own scale (its "100%"). */
    private static final Map<String, Double> SCALE_MAX = Map.of(
            TMDB, 10.0,
            IMDB, 10.0,
            ROTTEN_TOMATOES, 100.0,
            METACRITIC, 100.0);

    /**
     * @return the rating on the common 0-100 scale (always within {@code [0, 100]})
     * @throws UnknownRatingSourceException if the source has no defined scale
     * @throws NullPointerException         if {@code rating} is {@code null}
     */
    public NormalizedRating normalize(Rating rating) {
        Objects.requireNonNull(rating, "rating");

        Double scaleMax = SCALE_MAX.get(rating.source());
        if (scaleMax == null) {
            throw new UnknownRatingSourceException(rating.source());
        }

        double onHundred = rating.score() * 100.0 / scaleMax;
        double clamped = Math.max(0.0, Math.min(100.0, onHundred));
        return new NormalizedRating(rating, roundToOneDecimal(clamped));
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
