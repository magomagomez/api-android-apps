package com.magomez.androidapps.movierec.api;

import java.util.List;

/**
 * Body of {@code POST /api/movies/import}. Web contract only: it is deserialized from
 * JSON and then translated to domain {@code MovieQuery} objects by {@link MovieApiMapper}.
 *
 * <pre>
 * {
 *   "movies": [
 *     {"title": "The Substance", "year": 2024, "director": "Coralie Fargeat"},
 *     {"title": "Perfect Days", "year": 2023, "director": "Wim Wenders"}
 *   ]
 * }
 * </pre>
 *
 * @param movies the movies to identify; {@code title} is required, {@code year} and
 *               {@code director} are optional
 */
public record MovieImportRequest(List<Entry> movies) {

    public MovieImportRequest {
        movies = movies == null ? List.of() : List.copyOf(movies);
    }

    public record Entry(String title, Integer year, String director) {
    }
}
