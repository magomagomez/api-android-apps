package com.magomez.androidapps.movierec.recommendation;

import java.util.List;
import java.util.Objects;

/**
 * How strongly a candidate resembles films the user has already seen, as a single
 * observable number plus the matches it is built from.
 *
 * <p><b>Experimental — not used for ranking.</b> It never feeds {@code PersonalAffinity},
 * {@code PersonalMatchScore}, {@code affinityMatch} or the ordering. The
 * {@code RecommendationReasoner} may use it to phrase / prioritise the explanation, as it
 * already did with the raw matches.
 *
 * <p>Deterministic formula (see {@code SimilaritySignalCalculator}). {@code similarityStrength}
 * is dominated by the <em>best</em> single match, with a saturating bonus for extra
 * matches so 10 matches are not worth 10&times; one match.
 *
 * @param similarityStrength combined strength in {@code [0, 1]}
 * @param matchCount         number of matched watched movies
 * @param matches            the matches, strongest first (empty when the candidate
 *                           resembles nothing in the history)
 */
public record SimilaritySignal(double similarityStrength, int matchCount, List<SimilarityMatch> matches) {

    public SimilaritySignal {
        Objects.requireNonNull(matches, "matches");
        matches = List.copyOf(matches);
        matchCount = matches.size();
        if (!Double.isFinite(similarityStrength) || similarityStrength < 0.0 || similarityStrength > 1.0) {
            throw new IllegalArgumentException(
                    "similarityStrength must be within [0, 1]: " + similarityStrength);
        }
    }

    public static SimilaritySignal none() {
        return new SimilaritySignal(0.0, 0, List.of());
    }

    public boolean hasMatches() {
        return !matches.isEmpty();
    }
}
