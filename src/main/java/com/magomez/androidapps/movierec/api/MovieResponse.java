package com.magomez.androidapps.movierec.api;

import com.magomez.androidapps.movierec.model.Rating;

import java.util.List;

/**
 * HTTP response shape of an identified movie.
 *
 * <p>This is where the domain {@code Movie} (with its {@code Director} / {@code Actor}
 * / {@code Genre} / {@code Country} value objects) is flattened to the plain fields the
 * {@code /api/movies/import} contract exposes. Keeping this here means the domain model
 * stays free of any serialization concern.
 *
 * <p>{@code ratings} are exposed as-is (domain {@link Rating}: {@code source},
 * {@code score}, {@code voteCount}), in {@code Movie.ratings} order. {@code voteCount}
 * is {@code null} when the source did not report one. The list is empty when the movie
 * has no ratings.
 */
public record MovieResponse(
        Integer tmdbId,
        String imdbId,
        String title,
        String originalTitle,
        String releaseDate,
        Integer runtime,
        String overview,
        List<String> genres,
        String director,
        List<String> actors,
        List<String> countries,
        String poster,
        List<Rating> ratings
) {
}
