package com.magomez.androidapps.movierec.recommendation;

import java.util.List;
import java.util.Objects;

/**
 * A candidate that was removed before ranking, with the reason why.
 *
 * <p>Temporary and immutable; never persisted.
 *
 * @param title      the title exactly as it was requested
 * @param tmdbId     the resolved TMDB id when the candidate was identified, otherwise {@code null}
 * @param reason     the single reason this candidate is out of the ranking
 * @param candidates competing matches, present only when {@code reason == AMBIGUOUS}
 * @param note       optional human-readable diagnostic (e.g. a provider error message)
 */
public record ExcludedCandidate(
        String title,
        Integer tmdbId,
        ExclusionReason reason,
        List<String> candidates,
        String note) {

    public ExcludedCandidate {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(reason, "reason");
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public static ExcludedCandidate notFound(String title, String providerError) {
        return new ExcludedCandidate(title, null, ExclusionReason.NOT_FOUND, List.of(),
                providerError == null ? "no match found" : "provider error: " + providerError);
    }

    public static ExcludedCandidate ambiguous(String title, List<String> candidates) {
        return new ExcludedCandidate(title, null, ExclusionReason.AMBIGUOUS, candidates,
                "several plausible matches; provide year and/or director to disambiguate");
    }

    public static ExcludedCandidate alreadyWatched(String title, Integer tmdbId) {
        return new ExcludedCandidate(title, tmdbId, ExclusionReason.ALREADY_WATCHED, List.of(),
                "already in the Letterboxd library");
    }

    public static ExcludedCandidate duplicate(String title, Integer tmdbId) {
        return new ExcludedCandidate(title, tmdbId, ExclusionReason.DUPLICATE_CANDIDATE, List.of(),
                "another candidate in this request resolves to the same TMDB id");
    }
}
