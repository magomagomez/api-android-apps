package com.magomez.androidapps.movierec.scoring;

import com.magomez.androidapps.movierec.model.Country;
import com.magomez.androidapps.movierec.model.Genre;
import com.magomez.androidapps.movierec.model.Movie;
import com.magomez.androidapps.movierec.scoring.pattern.NarrativePattern;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Deterministic, pure computation of a movie's {@link PersonalMatchScore} for one
 * {@link UserTasteProfile}.
 *
 * <p>Steps:
 * <ol>
 *   <li>{@link QualityScoreCalculator} &rarr; {@link QualityScore};</li>
 *   <li>{@link PersonalAffinityCalculator} &rarr; {@link PersonalAffinitySignals}
 *       (director, genre, actor);</li>
 *   <li>{@code personalAffinity = director * 0.55 + genre * 0.35 + actor * 0.10} — only
 *       when the profile has at least one preference;</li>
 *   <li>{@code base = quality * 0.35 + personalAffinity * 0.65} — only when both halves
 *       are available (global quality deliberately weighs less than personal fit);</li>
 *   <li>{@code patternBonus = PATTERN_BONUS_MAX * maxPatternRelevance} — a small, bounded
 *       reward for candidates that fit one of the user's narrative taste patterns
 *       ({@code maxPatternRelevance} is the best {@code preferenceStrength * patternMatch}
 *       over the profile's patterns, in {@code [0, 1]} — {@code preferenceStrength}, not
 *       {@code strength}: a pattern that is merely common in the user's history (no real
 *       lift over their baseline) must not buy the same reward as one they actually
 *       prefer — see {@link TastePattern});</li>
 *   <li>{@code accoladeBonus = ACCOLADE_BONUS_MAX * accoladeStrength} — a smaller, bounded
 *       reward for external recognition (festival selections and awards), passed in
 *       already computed by {@code AccoladeSignalCalculator} — this class stays a pure
 *       function of primitives and never talks to a provider;</li>
 *   <li>{@code personalMatchScore = clamp(base + patternBonus + accoladeBonus, 0, 100)},
 *       rounded to one decimal — this is {@link PersonalMatchScore#value()}, present only
 *       with a trustworthy quality;</li>
 *   <li>{@link PersonalMatchScore#estimatedValue()} is the same computation with the
 *       quality term dropped (never guessed) when it is missing:
 *       {@code clamp(personalAffinity * 0.65 + patternBonus + accoladeBonus, 0, 100)} —
 *       present whenever the affinity half alone is known, so every candidate with a
 *       taste signal gets one honest, comparable ranking number.</li>
 * </ol>
 *
 * <p>Neither bonus touches the {@code 0.35/0.65} nor the {@code 0.55/0.35/0.10} weights.
 * {@code accoladeBonus} is capped lower than {@code patternBonus}: it rewards what
 * festival programmers or juries recognised, not what matches <em>this</em> user — the
 * personal-taste bonus stays the bigger lever. No LLM.
 */
@Component
public class PersonalMatchScoreCalculator {

    private static final double QUALITY_WEIGHT = 0.35;
    private static final double AFFINITY_WEIGHT = 0.65;

    private static final double DIRECTOR_AFFINITY_WEIGHT = 0.55;
    private static final double GENRE_AFFINITY_WEIGHT = 0.35;
    private static final double ACTOR_AFFINITY_WEIGHT = 0.10;

    /** Maximum points the taste-pattern bonus can add to the base score. */
    static final double PATTERN_BONUS_MAX = 30.0;
    /** Maximum points the accolade bonus can add — lower than the pattern bonus on purpose. */
    static final double ACCOLADE_BONUS_MAX = 20.0;

    private final QualityScoreCalculator qualityScoreCalculator;
    private final PersonalAffinityCalculator personalAffinityCalculator;

    public PersonalMatchScoreCalculator(QualityScoreCalculator qualityScoreCalculator,
                                        PersonalAffinityCalculator personalAffinityCalculator) {
        this.qualityScoreCalculator = qualityScoreCalculator;
        this.personalAffinityCalculator = personalAffinityCalculator;
    }

    /** Backward-compatible: no accolade signal available (defaults to {@code 0}). */
    public PersonalMatchScore calculate(Movie movie, UserTasteProfile profile) {
        return calculate(movie, profile, 0.0);
    }

    /**
     * @param accoladeStrength the candidate's {@code AccoladeSignal.strength()}, in
     *                         {@code [0, 1]}; {@code 0} when it carries no recognition
     */
    public PersonalMatchScore calculate(Movie movie, UserTasteProfile profile, double accoladeStrength) {
        Objects.requireNonNull(movie, "movie");
        Objects.requireNonNull(profile, "profile");
        if (!Double.isFinite(accoladeStrength) || accoladeStrength < 0.0 || accoladeStrength > 1.0) {
            throw new IllegalArgumentException(
                    "accoladeStrength must be in [0,1]: " + accoladeStrength);
        }

        QualityScore qualityScore = qualityScoreCalculator.calculate(movie);
        PersonalAffinitySignals signals = personalAffinityCalculator.calculate(movie, profile);

        OptionalDouble personalAffinity = hasAnyPreference(profile)
                ? OptionalDouble.of(roundToOneDecimal(weightedAffinity(signals)))
                : OptionalDouble.empty();

        double patternBonus = roundToOneDecimal(
                PATTERN_BONUS_MAX * maxPatternRelevance(movie, profile));
        double accoladeBonus = roundToOneDecimal(ACCOLADE_BONUS_MAX * accoladeStrength);
        double bonusSum = patternBonus + accoladeBonus;

        OptionalDouble value = OptionalDouble.empty();
        OptionalDouble estimatedValue = OptionalDouble.empty();
        if (personalAffinity.isPresent()) {
            double affinityContribution = personalAffinity.getAsDouble() * AFFINITY_WEIGHT;
            if (qualityScore.hasValue()) {
                double base = qualityScore.value().getAsDouble() * QUALITY_WEIGHT + affinityContribution;
                value = OptionalDouble.of(roundToOneDecimal(clamp(base + bonusSum)));
                estimatedValue = value;
            } else {
                // No trustworthy quality: give that 35% share zero credit rather than
                // inventing it, so the candidate still gets one honest ranking number.
                estimatedValue = OptionalDouble.of(roundToOneDecimal(clamp(affinityContribution + bonusSum)));
            }
        }

        return new PersonalMatchScore(
                value, estimatedValue, qualityScore, signals, personalAffinity, patternBonus, accoladeBonus);
    }

    /**
     * The best {@code preferenceStrength * patternMatch} over the profile's taste patterns
     * for this movie, in {@code [0, 1]}. Deliberately {@code preferenceStrength} (the pure
     * lift reading), not {@code strength} (which {@code TastePatternDetector} lets
     * prevalence inflate) — the bonus rewards fitting a narrative flavour the user actually
     * gravitates towards, not one that is merely a large, undifferentiated share of
     * everything they rate. {@code 0} when the movie matches none.
     */
    private static double maxPatternRelevance(Movie movie, UserTasteProfile profile) {
        List<String> genres = movie.genres().stream().map(Genre::name)
                .filter(n -> n != null && !n.isBlank()).toList();
        List<String> countries = movie.countries().stream().map(Country::name)
                .filter(n -> n != null && !n.isBlank()).toList();
        return profile.patterns().stream()
                .mapToDouble(tp -> definitionOf(tp.id())
                        .map(def -> tp.preferenceStrength() * def.match(genres, countries))
                        .orElse(0.0))
                .max()
                .orElse(0.0);
    }

    private static Optional<NarrativePattern> definitionOf(String id) {
        try {
            return Optional.of(NarrativePattern.valueOf(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static double weightedAffinity(PersonalAffinitySignals s) {
        return s.directorAffinity() * DIRECTOR_AFFINITY_WEIGHT
                + s.genreAffinity() * GENRE_AFFINITY_WEIGHT
                + s.actorAffinity() * ACTOR_AFFINITY_WEIGHT;
    }

    private static boolean hasAnyPreference(UserTasteProfile profile) {
        return !profile.preferredDirectors().isEmpty()
                || !profile.preferredGenres().isEmpty()
                || !profile.preferredActors().isEmpty();
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
