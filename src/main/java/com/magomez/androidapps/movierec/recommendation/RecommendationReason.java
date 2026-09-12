package com.magomez.androidapps.movierec.recommendation;

import java.util.List;
import java.util.Objects;

/**
 * A deterministic, transparent explanation of why a movie is recommended.
 *
 * <ul>
 *   <li>{@link #reasons()} — the structured signals, one short factual phrase each, every
 *       one backed by real profile / TMDB data (CLAUDE.md §17);</li>
 *   <li>{@link #text()} — those signals stitched into a short cinéfile paragraph;</li>
 *   <li>{@link #patternSignals()} — for every active taste pattern the film matches, its
 *       {@code profileStrength}, {@code patternMatch} and {@code relevance} (diagnostic).</li>
 * </ul>
 *
 * <p>No LLM is involved. A later phase may pass {@link #reasons()} to an LLM to produce a
 * more natural {@link #text()}, but the LLM will never decide the ranking nor add
 * information that is not already here.
 *
 * @param text           the composed explanation (never blank)
 * @param reasons        the individual signals, in priority order (may be empty)
 * @param patternSignals the matched taste patterns, ordered by {@code relevance} descending
 */
public record RecommendationReason(String text, List<String> reasons, List<PatternSignal> patternSignals) {

    public RecommendationReason {
        Objects.requireNonNull(text, "text");
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        patternSignals = patternSignals == null ? List.of() : List.copyOf(patternSignals);
    }

    public RecommendationReason(String text, List<String> reasons) {
        this(text, reasons, List.of());
    }
}
