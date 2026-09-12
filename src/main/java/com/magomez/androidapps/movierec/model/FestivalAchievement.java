package com.magomez.androidapps.movierec.model;

import java.util.Objects;

/**
 * A single piece of film-festival history for a movie: being selected in a section, or
 * winning an award there. Generic across festivals (Cannes, Venice, ...).
 *
 * <p>Domain value object: immutable, temporary, no transport / provider detail.
 * No {@code festivalScore} is computed from it.
 *
 * @param festival    festival name, e.g. {@code "Cannes"} (never blank)
 * @param editionYear the edition's year, e.g. {@code 2024}
 * @param section     the section, e.g. {@code "Compétition"} / {@code "Un Certain Regard"};
 *                    {@code null} when unknown
 * @param type        {@link Type#SELECTION} or {@link Type#AWARD}
 * @param awardName   the award's name (e.g. {@code "Palme d'Or"}) — required for
 *                    {@code AWARD}, must be {@code null} for {@code SELECTION}
 */
public record FestivalAchievement(
        String festival,
        int editionYear,
        String section,
        Type type,
        String awardName) {

    /** What the movie achieved at the festival. {@code OVATION} is deliberately not modelled yet. */
    public enum Type { SELECTION, AWARD }

    private static final int MIN_EDITION_YEAR = 1900;
    private static final int MAX_EDITION_YEAR = 2100;

    public FestivalAchievement {
        if (festival == null || festival.isBlank()) {
            throw new IllegalArgumentException("festival must not be blank");
        }
        festival = festival.trim();

        if (editionYear < MIN_EDITION_YEAR || editionYear > MAX_EDITION_YEAR) {
            throw new IllegalArgumentException("editionYear out of range: " + editionYear);
        }

        Objects.requireNonNull(type, "type");

        section = (section == null || section.isBlank()) ? null : section.trim();
        awardName = (awardName == null || awardName.isBlank()) ? null : awardName.trim();

        if (type == Type.AWARD && awardName == null) {
            throw new IllegalArgumentException("an AWARD achievement requires an awardName");
        }
        if (type == Type.SELECTION && awardName != null) {
            throw new IllegalArgumentException("a SELECTION achievement must not have an awardName");
        }
    }

    public static FestivalAchievement selection(String festival, int editionYear, String section) {
        return new FestivalAchievement(festival, editionYear, section, Type.SELECTION, null);
    }

    public static FestivalAchievement award(String festival, int editionYear, String section,
                                            String awardName) {
        return new FestivalAchievement(festival, editionYear, section, Type.AWARD, awardName);
    }

    public boolean isAward() {
        return type == Type.AWARD;
    }

    public boolean isSelection() {
        return type == Type.SELECTION;
    }
}
