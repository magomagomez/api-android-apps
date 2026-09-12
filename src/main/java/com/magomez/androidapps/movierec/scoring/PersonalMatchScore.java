package com.magomez.androidapps.movierec.scoring;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Deterministic PERSONAL MATCH SCORE.
 *
 * <p>Base blend, on the 0-100 scale:
 * <pre>base = quality * 0.35 + personalAffinity * 0.65</pre>
 * where {@code personalAffinity = director*0.55 + genre*0.35 + actor*0.10}. On top of the
 * base two small, bounded bonuses are added:
 * <pre>value = clamp(base + patternBonus + accoladeBonus, 0, 100)</pre>
 * {@link #patternBonus()} rewards fitting one of the user's own narrative taste patterns
 * (Korean thriller, uncomfortable cinema, …) — personal taste. {@link #accoladeBonus()}
 * rewards external recognition (festival selections and awards) — not personal, so it is
 * capped lower. Neither bonus changes the weights above; both are derived deterministically.
 *
 * <p>Immutable and explainable: it keeps the exact {@link QualityScore}, the three
 * individual {@link PersonalAffinitySignals} (director, genre, actor) and both bonuses.
 *
 * <p>Missing-data policy — nothing is invented:
 * <ul>
 *   <li>{@link #value()} is present <em>only</em> when both the quality score and the
 *       personal affinity are available — this is the number this project calls the
 *       PERSONAL MATCH SCORE, and it is never asserted without a trustworthy QUALITY;</li>
 *   <li>{@link #estimatedValue()} is present whenever the personal affinity alone is
 *       known. When quality is missing it gives that share of the blend <em>zero</em>
 *       credit rather than guessing — {@code affinityContribution + patternBonus +
 *       accoladeBonus}, clamped — so an unreleased film with strong personal fit ranks
 *       somewhere honest instead of being hidden below every quality-backed film,
 *       however weak. It equals {@link #value()} whenever that is present;</li>
 *   <li>the quality score is unavailable when the movie has no ratings;</li>
 *   <li>the personal affinity is unavailable when the profile carries no preference at all;</li>
 *   <li>{@link #status()} states which half (if any) is missing — the definitive way to
 *       tell whether a number is the confirmed PERSONAL MATCH SCORE or a provisional
 *       estimate.</li>
 * </ul>
 *
 * @param value            the combined score in {@code [0, 100]}, or empty when it
 *                         cannot be computed in full
 * @param estimatedValue   the best deterministic ranking estimate in {@code [0, 100]},
 *                         present whenever {@code personalAffinity} is; see above
 * @param qualityScore     the quality score used (its {@code value()} may itself be empty)
 * @param affinitySignals  the three individual affinity signals used (director, genre, actor)
 * @param personalAffinity the weighted blend {@code director*0.55 + genre*0.35 + actor*0.10},
 *                         or empty when the profile has no preferences
 * @param patternBonus     the taste-pattern bonus already folded into {@code value} /
 *                         {@code estimatedValue} ({@code 0} when no pattern matched)
 * @param accoladeBonus    the external-recognition bonus already folded into {@code value} /
 *                         {@code estimatedValue} ({@code 0} when the candidate carries no
 *                         accolade signal)
 */
public record PersonalMatchScore(
        OptionalDouble value,
        OptionalDouble estimatedValue,
        QualityScore qualityScore,
        PersonalAffinitySignals affinitySignals,
        OptionalDouble personalAffinity,
        double patternBonus,
        double accoladeBonus) {

    /** Which halves of the blend were available. */
    public enum Status { COMPLETE, MISSING_QUALITY, MISSING_AFFINITY, MISSING_BOTH }

    public PersonalMatchScore {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(estimatedValue, "estimatedValue");
        Objects.requireNonNull(qualityScore, "qualityScore");
        Objects.requireNonNull(affinitySignals, "affinitySignals");
        Objects.requireNonNull(personalAffinity, "personalAffinity");
        if (!Double.isFinite(patternBonus) || patternBonus < 0.0) {
            throw new IllegalArgumentException("patternBonus must be finite and >= 0: " + patternBonus);
        }
        if (!Double.isFinite(accoladeBonus) || accoladeBonus < 0.0) {
            throw new IllegalArgumentException("accoladeBonus must be finite and >= 0: " + accoladeBonus);
        }

        if (value.isPresent()) {
            double v = value.getAsDouble();
            if (!Double.isFinite(v) || v < 0.0 || v > 100.0) {
                throw new IllegalArgumentException(
                        "PERSONAL MATCH SCORE must be within [0, 100]: " + v);
            }
            if (!qualityScore.hasValue() || personalAffinity.isEmpty()) {
                throw new IllegalStateException(
                        "combined score present but a half is missing");
            }
            if (estimatedValue.isEmpty()) {
                throw new IllegalStateException("estimatedValue must be present whenever value is");
            }
        }
        if (estimatedValue.isPresent()) {
            double ev = estimatedValue.getAsDouble();
            if (!Double.isFinite(ev) || ev < 0.0 || ev > 100.0) {
                throw new IllegalArgumentException(
                        "estimatedValue must be within [0, 100]: " + ev);
            }
            if (personalAffinity.isEmpty()) {
                throw new IllegalStateException("estimatedValue present without a personalAffinity");
            }
        }
    }

    /**
     * Test constructor: both bonuses default to {@code 0}, {@code estimatedValue}
     * defaults to {@code value}.
     */
    public PersonalMatchScore(OptionalDouble value, QualityScore qualityScore,
                              PersonalAffinitySignals affinitySignals, OptionalDouble personalAffinity) {
        this(value, value, qualityScore, affinitySignals, personalAffinity, 0.0, 0.0);
    }

    public Status status() {
        boolean hasQuality = qualityScore.hasValue();
        boolean hasAffinity = personalAffinity.isPresent();
        if (hasQuality && hasAffinity) {
            return Status.COMPLETE;
        }
        if (!hasQuality && !hasAffinity) {
            return Status.MISSING_BOTH;
        }
        return hasQuality ? Status.MISSING_AFFINITY : Status.MISSING_QUALITY;
    }

    public boolean isComplete() {
        return status() == Status.COMPLETE;
    }
}
