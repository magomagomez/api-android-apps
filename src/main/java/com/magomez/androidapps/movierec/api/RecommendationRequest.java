package com.magomez.androidapps.movierec.api;

import java.util.List;

/**
 * Body of {@code POST /api/recommendations}. Web contract only: it is deserialized from
 * JSON and then translated to domain {@code MovieQuery} objects by
 * {@link RecommendationApiMapper}.
 *
 * <pre>
 * {
 *   "movies": [
 *     {"title": "The Substance", "year": 2024, "director": "Coralie Fargeat"},
 *     {"title": "Crash", "director": null}
 *   ]
 * }
 * </pre>
 *
 * <p>These are <b>candidate</b> movies to score against the user's taste, not an import.
 *
 * @param movies the candidates; {@code title} is required, {@code year} and
 *               {@code director} are optional
 */
public record RecommendationRequest(List<Entry> movies) {

    public RecommendationRequest {
        movies = movies == null ? List.of() : List.copyOf(movies);
    }

    public record Entry(String title, Integer year, String director) {
    }
}
