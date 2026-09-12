package com.magomez.androidapps.movierec.scoring;

import com.magomez.androidapps.movierec.model.Actor;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Deterministic, pure computation of the per-dimension {@link PersonalAffinitySignals}
 * for a {@link Movie} against a {@link UserTasteProfile}.
 *
 * <p>Three dimensions only — <b>director</b>, <b>genre</b>, <b>actor</b>. Country and
 * decade are not part of the PERSONAL MATCH SCORE and are not computed here.
 *
 * <p>It uses the <b>weighted affinities</b> stored in the profile (each in {@code [0, 1]}),
 * converted to the {@code [0, 100]} scale of the signals:
 * <ul>
 *   <li><b>director</b>: the user's stored weight for the movie's director, resolved by
 *       TMDB person id when available ({@link UserTasteProfile#directorAffinityOf});</li>
 *   <li><b>genre</b>: the arithmetic mean of the user's weight for each genre the movie
 *       carries (non-preferred = {@code 0});</li>
 *   <li><b>actor</b>: the arithmetic mean of the user's weight for each cast member the
 *       movie carries, again resolved by person id when available.</li>
 * </ul>
 *
 * <p>The mean means a movie cannot inflate a score just by carrying more genres/actors.
 * Missing data yields {@code 0}; no match is ever invented. No LLM, no APIs, no
 * persistence. The {@link Movie} is only read, never modified.
 */
@Component
public class PersonalAffinityCalculator {

    private static final double TO_HUNDRED = 100.0;

    public PersonalAffinitySignals calculate(Movie movie, UserTasteProfile profile) {
        Objects.requireNonNull(movie, "movie");
        Objects.requireNonNull(profile, "profile");

        return new PersonalAffinitySignals(
                onHundred(profile.directorAffinityOf(movie.director())),
                onHundred(meanGenreAffinity(names(movie.genres().stream().map(Genre::name)),
                        profile.genreAffinity())),
                onHundred(meanActorAffinity(movie.actors(), profile)));
    }

    /**
     * Combined affinity for the genres the movie carries: the arithmetic mean of the
     * user's weight for each value (non-preferred values count as {@code 0}). Returns
     * {@code 0} when the movie lists nothing or the user has no preference there.
     */
    private static double meanGenreAffinity(List<String> movieValues, Map<String, Double> affinities) {
        if (movieValues.isEmpty() || affinities.isEmpty()) {
            return 0.0;
        }
        Map<String, Double> normalized = normalizedAffinities(affinities);
        double total = movieValues.stream()
                .mapToDouble(value -> normalized.getOrDefault(normalize(value), 0.0))
                .sum();
        return total / movieValues.size();
    }

    /** Arithmetic mean of the user's per-actor weight (resolved by person id, else name). */
    private static double meanActorAffinity(List<Actor> cast, UserTasteProfile profile) {
        if (cast.isEmpty()) {
            return 0.0;
        }
        double total = cast.stream().mapToDouble(profile::actorAffinityOf).sum();
        return total / cast.size();
    }

    /** Re-keys the affinity map by normalized name; on a collision keeps the strongest weight. */
    private static Map<String, Double> normalizedAffinities(Map<String, Double> affinities) {
        Map<String, Double> out = new HashMap<>();
        affinities.forEach((key, weight) -> {
            String normalized = normalize(key);
            if (!normalized.isEmpty()) {
                out.merge(normalized, weight, Math::max);
            }
        });
        return out;
    }

    private static List<String> names(Stream<String> raw) {
        return raw.filter(name -> name != null && !name.isBlank()).toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static double onHundred(double affinity01) {
        return roundToOneDecimal(affinity01 * TO_HUNDRED);
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
