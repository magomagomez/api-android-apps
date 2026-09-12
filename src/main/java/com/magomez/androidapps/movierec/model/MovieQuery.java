package com.magomez.androidapps.movierec.model;

/**
 * A single request to identify a movie.
 *
 * <p>Temporary object: it only exists while a request is being processed.
 *
 * @param title    the movie title as provided by the user (never blank)
 * @param year     the release year when provided, otherwise {@code null}
 * @param director the director name when provided, otherwise {@code null}
 */
public record MovieQuery(String title, Integer year, String director) {

    public MovieQuery {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("MovieQuery title must not be blank");
        }
        title = title.trim();
        director = (director == null || director.isBlank()) ? null : director.trim();
    }

    public boolean hasYear() {
        return year != null;
    }

    public boolean hasDirector() {
        return director != null;
    }
}
