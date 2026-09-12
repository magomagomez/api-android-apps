package com.magomez.androidapps.movierec.model;

import java.util.List;
import java.util.Objects;

/**
 * Everything the external sources know about a movie's recognition, already in domain
 * language: an {@link AwardsTally} (aggregate counts, from OMDb) plus the specific
 * {@link FestivalAchievement}s (selections and awards, from Wikidata / festival archives).
 *
 * <p>Domain value object: immutable, temporary. It is the <em>raw</em> aggregate — the
 * {@code [0,1]} signal and the human-readable highlights are computed from it elsewhere.
 *
 * @param tally        aggregate award counts; {@link AwardsTally#empty()} when unknown
 * @param achievements specific festival selections / awards; empty when none is known
 */
public record AccoladeReport(AwardsTally tally, List<FestivalAchievement> achievements) {

    private static final AccoladeReport EMPTY = new AccoladeReport(AwardsTally.empty(), List.of());

    public AccoladeReport {
        Objects.requireNonNull(tally, "tally");
        achievements = achievements == null ? List.of() : List.copyOf(achievements);
    }

    public static AccoladeReport empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return tally.isEmpty() && achievements.isEmpty();
    }

    /** Merges two reports: counts are taken from the richer tally, achievements are unioned. */
    public AccoladeReport mergedWith(AccoladeReport other) {
        Objects.requireNonNull(other, "other");
        AwardsTally richerTally = other.tally.isEmpty() ? tally
                : tally.isEmpty() ? other.tally
                : (other.tally.wins() + other.tally.nominations()
                        > tally.wins() + tally.nominations() ? other.tally : tally);
        List<FestivalAchievement> union = new java.util.ArrayList<>(achievements);
        for (FestivalAchievement achievement : other.achievements) {
            if (!union.contains(achievement)) {
                union.add(achievement);
            }
        }
        return new AccoladeReport(richerTally, union);
    }
}
