package com.magomez.androidapps.movierec.model;

/**
 * Aggregate awards count for a movie, as a source like OMDb reports it in one line
 * ("{@code Won 1 Oscar. 9 wins & 23 nominations total}").
 *
 * <p>Domain value object: immutable, temporary, no transport / provider detail. It is a
 * <em>tally</em>, not a list of specific awards — which festival or academy gave what is
 * modelled by {@link FestivalAchievement}, filled from other sources. {@code oscarWins}
 * / {@code oscarNominations} are broken out because an Oscar is the single strongest
 * item in the line; they are already included in {@code wins} / {@code nominations}.
 *
 * @param oscarWins        Academy Award wins ({@code >= 0})
 * @param oscarNominations Academy Award nominations, wins excluded ({@code >= 0})
 * @param wins             total wins across all bodies, Oscars included ({@code >= 0})
 * @param nominations      total nominations, wins excluded ({@code >= 0})
 */
public record AwardsTally(int oscarWins, int oscarNominations, int wins, int nominations) {

    private static final AwardsTally EMPTY = new AwardsTally(0, 0, 0, 0);

    public AwardsTally {
        if (oscarWins < 0 || oscarNominations < 0 || wins < 0 || nominations < 0) {
            throw new IllegalArgumentException("award counts must not be negative");
        }
        if (oscarWins > wins) {
            throw new IllegalArgumentException(
                    "oscarWins (" + oscarWins + ") cannot exceed total wins (" + wins + ")");
        }
        if (oscarNominations > nominations) {
            throw new IllegalArgumentException("oscarNominations (" + oscarNominations
                    + ") cannot exceed total nominations (" + nominations + ")");
        }
    }

    public static AwardsTally empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return equals(EMPTY);
    }

    public boolean hasOscar() {
        return oscarWins > 0;
    }
}
