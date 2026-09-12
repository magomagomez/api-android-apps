package com.magomez.androidapps.movierec.api;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.IdentificationStatus;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.model.MovieIdentificationResult;
import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates between the HTTP contract ({@link MovieImportRequest} /
 * {@link MovieImportResponse}) and the domain ({@link MovieQuery} /
 * {@link MovieIdentificationResult}).
 *
 * <p>Pure and stateless, like {@code TmdbMovieMapper}. It is the single place that
 * knows both the JSON shape and the domain model, so neither {@code service/} nor
 * {@code model/} has to.
 */
public final class MovieApiMapper {

    private MovieApiMapper() {
    }

    /** Validates the request and converts it to domain queries. */
    public static List<MovieQuery> toQueries(MovieImportRequest request) {
        List<MovieImportRequest.Entry> entries = request == null ? List.of() : request.movies();

        List<MovieQuery> queries = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            MovieImportRequest.Entry entry = entries.get(i);
            if (entry == null || entry.title() == null || entry.title().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "movies[" + i + "].title is required");
            }
            queries.add(new MovieQuery(entry.title(), entry.year(), entry.director()));
        }
        return queries;
    }

    public static MovieImportResponse toResponse(List<MovieIdentificationResult> results) {
        List<MovieImportResponse.Item> items = results.stream()
                .map(MovieApiMapper::toItem)
                .toList();

        return new MovieImportResponse(
                items.size(),
                countOf(items, IdentificationStatus.IDENTIFIED),
                countOf(items, IdentificationStatus.NOT_FOUND),
                countOf(items, IdentificationStatus.AMBIGUOUS),
                items);
    }

    private static MovieImportResponse.Item toItem(MovieIdentificationResult result) {
        MovieMatch match = result.match();
        return new MovieImportResponse.Item(
                result.query().title(),
                result.query().year(),
                result.query().director(),
                match.status(),
                toMovieResponse(match.movie()),
                match.candidates(),
                noteFor(match.status(), result.identificationError()));
    }

    static MovieResponse toMovieResponse(Movie movie) {
        if (movie == null) {
            return null;
        }
        return new MovieResponse(
                movie.tmdbId(),
                movie.imdbId(),
                movie.title(),
                movie.originalTitle(),
                movie.releaseDate(),
                movie.runtime(),
                movie.overview(),
                movie.genres().stream().map(Genre::name).toList(),
                movie.director() == null ? null : movie.director().name(),
                movie.actors().stream().map(Actor::name).toList(),
                movie.countries().stream().map(Country::name).toList(),
                movie.poster(),
                movie.ratings()); // already ordered TMDB -> OMDb IMDb -> Rotten Tomatoes -> Metacritic
    }

    private static String noteFor(IdentificationStatus status, String identificationError) {
        if (identificationError != null) {
            return "provider error: " + identificationError;
        }
        return switch (status) {
            case IDENTIFIED -> null;
            case NOT_FOUND -> "no match found";
            case AMBIGUOUS -> "several plausible matches; provide year and/or director to disambiguate";
        };
    }

    private static int countOf(List<MovieImportResponse.Item> items, IdentificationStatus status) {
        return (int) items.stream().filter(i -> i.status() == status).count();
    }
}
