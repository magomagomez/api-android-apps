package com.magomez.androidapps.movierec.model;

/**
 * A movie TMDB relates to another one ("similar movies"). Only the little that is needed
 * to cross it against the user's history: the TMDB id, the title and the release year.
 *
 * <p>Temporary and immutable, like the rest of the domain; never persisted. It knows
 * nothing about HTTP, JSON or TMDB DTOs.
 *
 * @param tmdbId the related movie's TMDB id (never {@code null})
 * @param title  the related movie's title as TMDB returned it, or {@code null}
 * @param year   the related movie's release year, or {@code null}
 */
public record SimilarMovie(Integer tmdbId, String title, Integer year) {

    public SimilarMovie {
        if (tmdbId == null) {
            throw new IllegalArgumentException("SimilarMovie tmdbId must not be null");
        }
        title = (title == null || title.isBlank()) ? null : title.trim();
    }
}
