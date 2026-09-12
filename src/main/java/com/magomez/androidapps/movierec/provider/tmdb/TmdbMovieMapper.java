package com.magomez.androidapps.movierec.provider.tmdb;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Director;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.Rating;
import com.magomez.androidapps.movierec.model.SimilarMovie;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure transformation from a TMDB details payload to the internal domain model
 * ({@link Movie}, {@link Rating}).
 *
 * <p>No HTTP, no side effects: fully unit-testable.
 */
public final class TmdbMovieMapper {

    static final String POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";
    static final String RATING_SOURCE = "TMDB";
    private static final String DIRECTOR_JOB = "Director";
    private static final int MAX_ACTORS = 10;
    private static final int MAX_SIMILAR = 20;

    private TmdbMovieMapper() {
    }

    public static Movie toMovie(TmdbMovieDetails details) {
        Objects.requireNonNull(details, "details");
        return new Movie(
                details.id(),
                emptyToNull(details.imdbId()),
                details.title(),
                details.originalTitle(),
                details.releaseDate(),
                details.runtime(),
                details.overview(),
                genres(details),
                director(details),
                actors(details),
                countries(details),
                poster(details),
                List.of(), // ratings are enriched separately, never fetched by toMovie()
                similarMovies(details));
    }

    /**
     * Maps TMDB's {@code vote_average} / {@code vote_count} to a domain {@link Rating}
     * with {@code source = "TMDB"}, on TMDB's own 0-10 scale (no normalization).
     *
     * <p>Returns {@link Optional#empty()} when TMDB reports no usable rating: no
     * {@code vote_average}, or an explicit zero {@code vote_count}. A {@code null}
     * {@code vote_count} is kept as an unknown vote count on the {@link Rating}.
     */
    public static Optional<Rating> toRating(TmdbMovieDetails details) {
        Objects.requireNonNull(details, "details");
        Double average = details.voteAverage();
        Integer votes = details.voteCount();
        if (average == null || (votes != null && votes == 0)) {
            return Optional.empty();
        }
        return Optional.of(new Rating(RATING_SOURCE, average, votes));
    }

    private static List<Genre> genres(TmdbMovieDetails details) {
        if (details.genres() == null) {
            return List.of();
        }
        return details.genres().stream()
                .filter(g -> g.name() != null)
                .map(g -> new Genre(g.id(), g.name()))
                .toList();
    }

    private static List<Country> countries(TmdbMovieDetails details) {
        if (details.productionCountries() == null) {
            return List.of();
        }
        return details.productionCountries().stream()
                .filter(c -> c.name() != null)
                .map(c -> new Country(c.code(), c.name()))
                .toList();
    }

    private static Director director(TmdbMovieDetails details) {
        if (details.credits() == null || details.credits().crew() == null) {
            return null;
        }
        return details.credits().crew().stream()
                .filter(c -> DIRECTOR_JOB.equalsIgnoreCase(c.job()))
                .filter(c -> c.name() != null)
                .map(c -> new Director(c.id(), c.name()))
                .findFirst()
                .orElse(null);
    }

    private static List<Actor> actors(TmdbMovieDetails details) {
        if (details.credits() == null || details.credits().cast() == null) {
            return List.of();
        }
        return details.credits().cast().stream()
                .sorted(Comparator.comparingInt(
                        c -> c.order() == null ? Integer.MAX_VALUE : c.order()))
                .filter(c -> c.name() != null)
                .map(c -> new Actor(c.id(), c.name()))
                .limit(MAX_ACTORS)
                .toList();
    }

    /**
     * TMDB's raw "similar" list is noisy (popularity-driven), so we keep only entries
     * that share at least one genre with this film — closer to real similarity.
     */
    private static List<SimilarMovie> similarMovies(TmdbMovieDetails details) {
        if (details.similar() == null || details.similar().results() == null) {
            return List.of();
        }
        Set<Integer> ownGenreIds = details.genres() == null ? Set.of()
                : details.genres().stream()
                        .map(TmdbMovieDetails.TmdbGenre::id)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableSet());
        return details.similar().results().stream()
                .filter(s -> s.id() != null)
                .filter(s -> ownGenreIds.isEmpty() || sharesGenre(s.genreIds(), ownGenreIds))
                .map(s -> new SimilarMovie(s.id(), s.title(), releaseYear(s.releaseDate())))
                .limit(MAX_SIMILAR)
                .toList();
    }

    private static boolean sharesGenre(List<Integer> genreIds, Set<Integer> ownGenreIds) {
        return genreIds != null && genreIds.stream().anyMatch(ownGenreIds::contains);
    }

    private static Integer releaseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }
        try {
            return Integer.valueOf(releaseDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String poster(TmdbMovieDetails details) {
        return details.posterPath() == null ? null : POSTER_BASE_URL + details.posterPath();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
