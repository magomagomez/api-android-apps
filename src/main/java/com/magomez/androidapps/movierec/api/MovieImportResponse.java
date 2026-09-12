package com.magomez.androidapps.movierec.api;

import com.magomez.androidapps.movierec.model.IdentificationStatus;

import java.util.List;

/**
 * Response of {@code POST /api/movies/import}. Web contract only, built from the domain
 * results by {@link MovieApiMapper}.
 *
 * @param total      number of movies in the request
 * @param identified number of entries resolved to exactly one movie
 * @param notFound   number of entries with no match
 * @param ambiguous  number of entries with several plausible matches
 * @param movies     one entry per requested movie, in the original order
 */
public record MovieImportResponse(
        int total,
        int identified,
        int notFound,
        int ambiguous,
        List<Item> movies
) {

    public MovieImportResponse {
        movies = movies == null ? List.of() : List.copyOf(movies);
    }

    /**
     * Per-entry result.
     *
     * @param query      the title requested
     * @param year       the year requested, or {@code null}
     * @param director   the director requested, or {@code null}
     * @param status     identification outcome for this entry
     * @param movie      the identified movie, present only when {@code status == IDENTIFIED}
     * @param candidates competing matches, present only when {@code status == AMBIGUOUS}
     * @param note       optional human readable diagnostic
     */
    public record Item(
            String query,
            Integer year,
            String director,
            IdentificationStatus status,
            MovieResponse movie,
            List<String> candidates,
            String note
    ) {

        public Item {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }
    }
}
