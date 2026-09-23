package com.magomez.androidapps.movierec.recommendation;

import com.magomez.androidapps.movierec.scoring.PersonalMatchScore;
import com.magomez.androidapps.movierec.scoring.TastePattern;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of the recommendation pipeline for one request.
 *
 * <ul>
 *   <li>{@link #recommendations} — the <b>TOP {@value RecommendationService#TOP_N}</b>
 *       (or fewer, when there aren't that many eligible candidates) in <b>one</b> list,
 *       ordered by {@code PersonalMatchScore.estimatedValue()} descending — this is the
 *       project's actual deliverable, not every identified, unseen candidate. A candidate
 *       with a trustworthy external QUALITY carries the confirmed PERSONAL MATCH SCORE
 *       ({@code PersonalMatchScore.value()}); one without still competes for a place on
 *       that same scale, giving the unproven quality share zero credit rather than
 *       guessing it. {@code status()} on each candidate's score says which is which — see
 *       {@link PersonalMatchScore};</li>
 *   <li>{@link #excluded} — {@code NOT_FOUND} / {@code AMBIGUOUS} / already watched / duplicate.</li>
 * </ul>
 *
 * <p>Temporary and immutable; never persisted.
 *
 * @param totalCandidates how many candidates the request contained
 * @param recommendations the top {@value RecommendationService#TOP_N}, {@code estimatedValue} descending
 * @param excluded        candidates removed before ranking, in request order
 * @param profilePatterns the narrative taste patterns detected in the user's history
 */
public record RecommendationResult(
        int totalCandidates,
        List<ScoredCandidate> recommendations,
        List<ExcludedCandidate> excluded,
        List<TastePattern> profilePatterns) {

    public RecommendationResult {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        excluded = excluded == null ? List.of() : List.copyOf(excluded);
        profilePatterns = profilePatterns == null ? List.of() : List.copyOf(profilePatterns);
    }

    /** Backward-compatible constructor without profile patterns. */
    public RecommendationResult(int totalCandidates, List<ScoredCandidate> recommendations,
                                List<ExcludedCandidate> excluded) {
        this(totalCandidates, recommendations, excluded, List.of());
    }

    /**
     * Candidates with a trustworthy external QUALITY: a confirmed PERSONAL MATCH SCORE.
     * A filtered view over {@link #recommendations} — same order.
     */
    public List<ScoredCandidate> ranked() {
        return recommendations.stream()
                .filter(c -> c.score().status() == PersonalMatchScore.Status.COMPLETE)
                .toList();
    }

    /**
     * Candidates without a trustworthy external QUALITY: ranked by the same
     * {@code estimatedValue}, just never backed by a confirmed PERSONAL MATCH SCORE.
     * A filtered view over {@link #recommendations} — same order.
     */
    public List<ScoredCandidate> affinityOnly() {
        return recommendations.stream()
                .filter(c -> c.score().status() != PersonalMatchScore.Status.COMPLETE)
                .toList();
    }

    public long excludedFor(ExclusionReason reason) {
        Objects.requireNonNull(reason, "reason");
        return excluded.stream().filter(e -> e.reason() == reason).count();
    }
}
