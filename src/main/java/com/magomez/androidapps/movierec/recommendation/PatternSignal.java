package com.magomez.androidapps.movierec.recommendation;

import java.util.Objects;

/**
 * How one of the user's narrative {@link com.magomez.androidapps.movierec.scoring.TastePattern}s
 * relates to a concrete candidate film — the two things kept deliberately separate:
 *
 * <ul>
 *   <li>{@link #profileStrength} — how much the pattern characterises the user's taste
 *       (from their Letterboxd history);</li>
 *   <li>{@link #patternMatch} — how strongly this particular film fits the pattern
 *       (from the film's own TMDB genres / country);</li>
 *   <li>{@link #relevance} = {@code profileStrength * patternMatch} — the combined
 *       "why this pattern matters for recommending this film".</li>
 * </ul>
 *
 * <p>Diagnostic only for now: {@code relevance} orders which pattern the explanation
 * leads with, but nothing here feeds the PERSONAL MATCH SCORE or the ranking.
 *
 * @param id             the {@code NarrativePattern} id (e.g. {@code "KOREAN_THRILLER"})
 * @param name           short human name
 * @param profileStrength {@code [0, 1]}
 * @param patternMatch    {@code (0, 1]} (this record is only produced when the film matches)
 * @param relevance       {@code [0, 1]}, {@code profileStrength * patternMatch}, rounded to 4 dp
 */
public record PatternSignal(
        String id,
        String name,
        double profileStrength,
        double patternMatch,
        double relevance) {

    public PatternSignal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    public static PatternSignal of(String id, String name, double profileStrength, double patternMatch) {
        double relevance = Math.round(profileStrength * patternMatch * 10_000.0) / 10_000.0;
        return new PatternSignal(id, name, profileStrength, patternMatch, relevance);
    }
}
