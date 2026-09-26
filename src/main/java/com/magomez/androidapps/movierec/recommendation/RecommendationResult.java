package com.magomez.androidapps.movierec.recommendation;

import com.magomez.androidapps.movierec.scoring.PersonalMatchScore;
import com.magomez.androidapps.movierec.scoring.TastePattern;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of the recommendation pipeline for one request.
 *
 * <ul>
 *   <li>{@link #recommendations} — the <b>general TOP {@value RecommendationService#TOP_N}</b>
 *       (or fewer, when there aren't that many eligible candidates) in <b>one</b> list,
 *       ordered by {@code PersonalMatchScore.estimatedValue()} descending — this is the
 *       project's actual deliverable, not every identified, unseen candidate. A candidate
 *       with a trustworthy external QUALITY carries the confirmed PERSONAL MATCH SCORE
 *       ({@code PersonalMatchScore.value()}); one without still competes for a place on
 *       that same scale, giving the unproven quality share zero credit rather than
 *       guessing it. {@code status()} on each candidate's score says which is which — see
 *       {@link PersonalMatchScore};</li>
 *   <li>{@link #scheduleRecommendations} — the same ranking, but restricted first to
 *       candidates with at least one screening in the festival programme at a convenient
 *       time ({@code SchedulePriority.isConvenient}), then capped to the same TOP
 *       {@value RecommendationService#TOP_N}. A film absent from the programme, or only
 *       screening at an inconvenient time, never appears here even if it would have
 *       ranked in the general list;</li>
 *   <li>{@link #excluded} — {@code NOT_FOUND} / {@code AMBIGUOUS} / already watched / duplicate.</li>
 * </ul>
 *
 * <p>Temporary and immutable; never persisted.
 *
 * @param totalCandidates        how many candidates the request contained
 * @param recommendations        the general top {@value RecommendationService#TOP_N}, {@code estimatedValue} descending
 * @param scheduleRecommendations the schedule-compatible top {@value RecommendationService#TOP_N}
 * @param excluded               candidates removed before ranking, in request order
 * @param profilePatterns        the narrative taste patterns detected in the user's history
 */
public record RecommendationResult(
        int totalCandidates,
        List<ScoredCandidate> recommendations,
        List<ScoredCandidate> scheduleRecommendations,
        List<ExcludedCandidate> excluded,
        List<TastePattern> profilePatterns) {

    public RecommendationResult {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        scheduleRecommendations = scheduleRecommendations == null ? List.of() : List.copyOf(scheduleRecommendations);
        excluded = excluded == null ? List.of() : List.copyOf(excluded);
        profilePatterns = profilePatterns == null ? List.of() : List.copyOf(profilePatterns);
    }

    /** Backward-compatible constructor without a schedule-filtered list. */
    public RecommendationResult(int totalCandidates, List<ScoredCandidate> recommendations,
                                List<ExcludedCandidate> excluded, List<TastePattern> profilePatterns) {
        this(totalCandidates, recommendations, List.of(), excluded, profilePatterns);
    }

    /** Backward-compatible constructor without profile patterns or a schedule-filtered list. */
    public RecommendationResult(int totalCandidates, List<ScoredCandidate> recommendations,
                                List<ExcludedCandidate> excluded) {
        this(totalCandidates, recommendations, List.of(), excluded, List.of());
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
