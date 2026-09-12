package com.magomez.androidapps.movierec.model;

/**
 * An external rating of a movie. Domain value object: immutable, temporary and free of
 * any transport, serialization or provider detail.
 *
 * <p>It records only what a source reported. It deliberately does <em>not</em> assume a
 * common scale across sources: an IMDb {@code 8.1} (out of 10) and a Metacritic
 * {@code 74} (out of 100) are both valid {@code Rating}s and must not be compared
 * directly until a later phase introduces explicit scales and normalization.
 *
 * <p>No source is wired yet; this is only the domain concept.
 *
 * @param source    generic identifier of the rating source, e.g. {@code "IMDb"} or
 *                  {@code "TMDB"} (never blank)
 * @param score     the value reported by the source, on that source's own scale
 *                  (finite and not negative)
 * @param voteCount number of votes backing the score when the source reports it,
 *                  otherwise {@code null} (never negative)
 */
public record Rating(String source, double score, Integer voteCount) {

    public Rating {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Rating source must not be blank");
        }
        source = source.trim();
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("Rating score must be a finite number: " + score);
        }
        if (score < 0) {
            throw new IllegalArgumentException("Rating score must not be negative: " + score);
        }
        if (voteCount != null && voteCount < 0) {
            throw new IllegalArgumentException("Rating voteCount must not be negative: " + voteCount);
        }
    }

    /** Rating whose source did not report a vote count. */
    public static Rating of(String source, double score) {
        return new Rating(source, score, null);
    }

    public boolean hasVoteCount() {
        return voteCount != null;
    }
}
