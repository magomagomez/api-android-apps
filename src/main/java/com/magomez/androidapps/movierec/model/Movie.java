package com.magomez.androidapps.movierec.model;

import java.util.List;

/**
 * Temporary, immutable representation of an identified movie.
 *
 * <p>This is a domain object built from whatever provider identified the movie. It
 * knows nothing about HTTP, JSON payloads, Jackson or any concrete external API. It is
 * never persisted: it only lives while the import request is being processed.
 *
 * <p>The cinematographic entities are modelled as value objects ({@link Director},
 * {@link Actor}, {@link Genre}, {@link Country}, {@link Rating}) rather than plain
 * strings, so later phases (relations, awards, recommendation signals) can enrich them
 * without reshaping the core model. Turning this model into the HTTP response shape is
 * the job of the API layer.
 *
 * <p>{@code ratings} holds the external ratings gathered for this movie, on each
 * source's own scale ({@link Rating#source()} stays generic). It is empty until an
 * enrichment step (e.g. {@code MovieDataProvider.fetchRating}) attaches ratings via
 * {@link #withRatings(List)}; ratings are never fetched automatically here.
 *
 * <p>{@code similarMovies} holds TMDB's "similar movies" for this film (id + title +
 * year only). It is used to explain a recommendation when one of them is also in the
 * user's history; it never influences the score.
 *
 * <p>Not every field is available for every movie; missing values are {@code null}
 * (or empty lists).
 */
public record Movie(
        Integer tmdbId,
        String imdbId,
        String title,
        String originalTitle,
        String releaseDate,
        Integer runtime,
        String overview,
        List<Genre> genres,
        Director director,
        List<Actor> actors,
        List<Country> countries,
        String poster,
        List<Rating> ratings,
        List<SimilarMovie> similarMovies
) {

    public Movie {
        genres = genres == null ? List.of() : List.copyOf(genres);
        actors = actors == null ? List.of() : List.copyOf(actors);
        countries = countries == null ? List.of() : List.copyOf(countries);
        ratings = ratings == null ? List.of() : List.copyOf(ratings);
        similarMovies = similarMovies == null ? List.of() : List.copyOf(similarMovies);
    }

    /**
     * Backward-compatible constructor without {@code similarMovies} (defaults to empty).
     * Kept so the many existing call sites that predate TMDB "similar movies" still compile.
     */
    public Movie(Integer tmdbId, String imdbId, String title, String originalTitle,
                 String releaseDate, Integer runtime, String overview, List<Genre> genres,
                 Director director, List<Actor> actors, List<Country> countries, String poster,
                 List<Rating> ratings) {
        this(tmdbId, imdbId, title, originalTitle, releaseDate, runtime, overview, genres,
                director, actors, countries, poster, ratings, List.of());
    }

    /**
     * Returns a copy of this movie carrying the given ratings. The original instance is
     * left untouched; the copied ratings list is unmodifiable.
     */
    public Movie withRatings(List<Rating> ratings) {
        return new Movie(tmdbId, imdbId, title, originalTitle, releaseDate, runtime, overview,
                genres, director, actors, countries, poster, ratings, similarMovies);
    }

    /**
     * Returns a copy of this movie carrying the given TMDB similar movies. The original
     * instance is left untouched; the copied list is unmodifiable.
     */
    public Movie withSimilarMovies(List<SimilarMovie> similarMovies) {
        return new Movie(tmdbId, imdbId, title, originalTitle, releaseDate, runtime, overview,
                genres, director, actors, countries, poster, ratings, similarMovies);
    }

    /**
     * Returns a copy of this movie with the director and cast replaced (e.g. after a
     * name spelling has been resolved). The original instance is left untouched.
     */
    public Movie withDirectorAndActors(Director director, List<Actor> actors) {
        return new Movie(tmdbId, imdbId, title, originalTitle, releaseDate, runtime, overview,
                genres, director, actors, countries, poster, ratings, similarMovies);
    }
}
