package com.magomez.androidapps.movierec.provider.wikidata;

/**
 * One row of Wikidata's answer for a film: an award it received or a nomination it got,
 * with whatever context the query could pin down. Provider-internal — mapped to a domain
 * {@code FestivalAchievement} by {@link WikidataAccoladeProvider}.
 *
 * @param won            {@code true} for an award received, {@code false} for a nomination
 * @param awardLabel     the award's English label, e.g. {@code "Palme d'Or"} (never null)
 * @param conferrerLabel the "conferred by" entity's label (often the festival), or null
 * @param year           the year on the statement's "point in time" qualifier, or null
 * @param publicationYear the film's release year, used as a fallback edition year, or null
 */
public record WikidataAward(
        boolean won,
        String awardLabel,
        String conferrerLabel,
        Integer year,
        Integer publicationYear) {
}
