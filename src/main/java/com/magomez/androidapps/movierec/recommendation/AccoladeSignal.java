package com.magomez.androidapps.movierec.recommendation;

import com.magomez.androidapps.movierec.model.AwardsTally;
import com.magomez.androidapps.movierec.model.FestivalAchievement;

import java.util.List;
import java.util.Objects;

/**
 * How much external <em>recognition</em> a candidate carries, as a single deterministic
 * {@code [0,1]} strength plus the raw evidence behind it. Sibling of
 * {@link SimilaritySignal} / {@link PatternSignal}: observable on its own, and (from a
 * later phase) a bounded input to the PERSONAL MATCH SCORE.
 *
 * <p>{@code strength} is <em>not</em> a personal-taste signal — it says "this film was
 * widely honoured", not "this film matches you".
 *
 * @param strength     {@code [0,1]}, the stronger of award volume, Oscar standing and
 *                     festival standing (see {@link AccoladeSignalCalculator})
 * @param highlights   short human-readable phrases ("Ganó el Óscar", "Palme d'Or en
 *                     Cannes 2024"); may be empty
 * @param tally        the aggregate award counts behind the signal
 * @param achievements the specific festival selections / awards behind the signal
 */
public record AccoladeSignal(
        double strength,
        List<String> highlights,
        AwardsTally tally,
        List<FestivalAchievement> achievements) {

    private static final AccoladeSignal NONE =
            new AccoladeSignal(0.0, List.of(), AwardsTally.empty(), List.of());

    public AccoladeSignal {
        if (!Double.isFinite(strength) || strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("strength must be in [0,1]: " + strength);
        }
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
        Objects.requireNonNull(tally, "tally");
        achievements = achievements == null ? List.of() : List.copyOf(achievements);
    }

    public static AccoladeSignal none() {
        return NONE;
    }

    public boolean hasRecognition() {
        return strength > 0.0;
    }
}
