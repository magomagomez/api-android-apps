package com.magomez.androidapps.movierec.scoring.letterboxd;

import java.util.List;

/**
 * The little we keep about a movie the user has already seen on Letterboxd, so a
 * recommendation can name it ("hay similitudes claras con X, que está en tu historial")
 * and validate that a "similar" match is real (same director, or &ge; 2 shared genres).
 *
 * <p>Temporary and immutable; never persisted.
 *
 * @param tmdbId       the movie's TMDB id (never {@code null})
 * @param title        the movie's title as our identification resolved it, or {@code null}
 * @param userScore    the user's Letterboxd rating on the 0-10 scale
 * @param genres       the movie's genre names (may be empty)
 * @param directorName the movie's director name (already romanized), or {@code null}
 */
public record WatchedMovie(Integer tmdbId, String title, double userScore, List<String> genres,
                           String directorName) {

    public WatchedMovie {
        if (tmdbId == null) {
            throw new IllegalArgumentException("WatchedMovie tmdbId must not be null");
        }
        title = (title == null || title.isBlank()) ? null : title.trim();
        if (!Double.isFinite(userScore) || userScore < 0.0 || userScore > 10.0) {
            throw new IllegalArgumentException("userScore must be within [0, 10]: " + userScore);
        }
        genres = genres == null ? List.of() : List.copyOf(genres);
        directorName = (directorName == null || directorName.isBlank()) ? null : directorName.trim();
    }

    public WatchedMovie(Integer tmdbId, String title, double userScore, List<String> genres) {
        this(tmdbId, title, userScore, genres, null);
    }

    public WatchedMovie(Integer tmdbId, String title, double userScore) {
        this(tmdbId, title, userScore, List.of(), null);
    }
}
