package com.magomez.androidapps.movierec.scoring;

import java.util.List;
import java.util.Objects;

/**
 * A narrative taste pattern derived from the user's Letterboxd history — a recurring kind
 * of film among their best-rated movies (e.g. "Korean thriller", "black comedy",
 * "psychological horror").
 *
 * <p>Fully deterministic and transparent: it is only created when there is real evidence
 * in the history, its {@link #strength()} is a computed number in {@code [0, 1]}, and
 * {@link #indicators()} spells out <em>why</em> it fired (counts, recurring directors,
 * example movies). No LLM, no invented preferences.
 *
 * <p>{@link #strength()} answers "how present is this in your history" — it can be high
 * either because you clearly prefer it over your baseline (lift) or simply because you
 * rate a lot of it either way (prevalence), whichever is higher; that is deliberate, so a
 * pattern the user genuinely watches a lot of still shows up and gets explained.
 * {@link #preferenceStrength()} answers a narrower question — "do you actually prefer this
 * over your baseline" (the lift reading alone, never boosted by prevalence) — and is what
 * feeds the PERSONAL MATCH SCORE's pattern bonus: a pattern that is merely common in your
 * whole history (lift ≈ 1, e.g. "psychological horror" appearing exactly as often among
 * your favourites as everywhere else) should not hand out the same reward as one you
 * genuinely gravitate towards.
 *
 * <p>Immutable, no external dependencies, never persisted. Country and decade are not
 * part of the PERSONAL MATCH SCORE; a pattern may still use the country of a film as a
 * defining trait (e.g. Korean thriller), but only for explanation, never for scoring.
 *
 * @param id                 stable identifier (the {@code NarrativePattern} constant name,
 *                            e.g. {@code "KOREAN_THRILLER"})
 * @param name               short human name ("Thriller coreano")
 * @param description         one line describing what the pattern captures
 * @param strength            how strongly the history supports this pattern, in {@code [0, 1]}
 *                            (the better of "you prefer it" and "you see a lot of it")
 * @param preferenceStrength  how much you actually prefer this over your baseline, in
 *                            {@code [0, 1]} — the lift reading alone; this is what the PMS
 *                            pattern bonus uses, so mere volume can't buy a big reward
 * @param supportingGenres    the pattern's trigger genres actually seen in the history,
 *                            most frequent first
 * @param supportingDirectors directors of the user's matching favourites, most frequent first
 * @param supportingMovies    titles of the user's matching favourites, best-rated first
 * @param indicators          human-readable evidence lines (transparency, not a black box)
 */
public record TastePattern(
        String id,
        String name,
        String description,
        double strength,
        double preferenceStrength,
        List<String> supportingGenres,
        List<String> supportingDirectors,
        List<String> supportingMovies,
        List<String> indicators) {

    public TastePattern {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        if (!Double.isFinite(strength) || strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("strength must be within [0, 1]: " + strength);
        }
        if (!Double.isFinite(preferenceStrength) || preferenceStrength < 0.0 || preferenceStrength > 1.0) {
            throw new IllegalArgumentException(
                    "preferenceStrength must be within [0, 1]: " + preferenceStrength);
        }
        supportingGenres = supportingGenres == null ? List.of() : List.copyOf(supportingGenres);
        supportingDirectors = supportingDirectors == null ? List.of() : List.copyOf(supportingDirectors);
        supportingMovies = supportingMovies == null ? List.of() : List.copyOf(supportingMovies);
        indicators = indicators == null ? List.of() : List.copyOf(indicators);
    }

    /**
     * Backward-compatible: {@code preferenceStrength} defaults to {@code strength} (assumes
     * the pattern is distinctive when the caller doesn't say otherwise — the common case in
     * hand-built fixtures that don't exercise the lift/prevalence distinction).
     */
    public TastePattern(String id, String name, String description, double strength,
                        List<String> supportingGenres, List<String> supportingDirectors,
                        List<String> supportingMovies, List<String> indicators) {
        this(id, name, description, strength, strength, supportingGenres, supportingDirectors,
                supportingMovies, indicators);
    }
}
