package com.magomez.androidapps.movierec.recommendation;

import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Keeps the recommendation lists to feature-length live-action films: animation and
 * documentaries are excluded by genre, short films by runtime. This is a deliberate,
 * fixed personal preference (like {@code UserTasteProfileBuilder}'s Drama exclusion) —
 * not a data-quality signal, so nothing here is configurable per user or per request.
 *
 * <p>TV series never reach this filter in the first place: identification only ever
 * searches TMDB's {@code /search/movie}, never {@code /search/tv} (see
 * {@code TmdbClient#searchMovies}), so nothing of that kind is ever identified here.
 *
 * <p>Genre names come back in whatever language TMDB movie details were queried in
 * ({@code es-ES} here — see {@code TmdbClient#LANGUAGE}), so the match is accent- and
 * case-insensitive rather than tied to one exact spelling.
 */
final class ContentFormatFilter {

    /**
     * The Academy's own cutoff for what counts as a "short film": 40 minutes or less,
     * credits included. Used as a runtime proxy since TMDB has no "Short Film" genre.
     */
    static final int SHORT_FILM_MAX_MINUTES = 40;

    private static final Set<String> EXCLUDED_GENRES =
            Set.of("animacion", "animation", "documental", "documentary");

    private ContentFormatFilter() {
    }

    /**
     * {@code null} when {@code movie} is a regular feature film that should be scored
     * normally; otherwise a short, human-readable reason it was left out.
     */
    static String excludedFormatReason(Movie movie) {
        for (Genre genre : movie.genres()) {
            if (genre.name() != null && EXCLUDED_GENRES.contains(normalize(genre.name()))) {
                return "género excluido: " + genre.name();
            }
        }
        Integer runtime = movie.runtime();
        if (runtime != null && runtime > 0 && runtime <= SHORT_FILM_MAX_MINUTES) {
            return "cortometraje (" + runtime + " min)";
        }
        return null;
    }

    private static String normalize(String value) {
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
