package com.magomez.androidapps.movierec.provider.cannes;

/**
 * One raw row from the Cannes archive for a film, as a {@link CannesArchiveSource}
 * would return it before it is turned into a domain {@code FestivalAchievement}.
 *
 * <p>Deliberately permissive (no validation): it is the messy edge between the
 * (future) HTML scraper and the domain. Interpretation:
 * <ul>
 *   <li>{@code awardName == null} / blank &rarr; a plain selection in {@code section};</li>
 *   <li>{@code awardName} present &rarr; that award, won in {@code section}.</li>
 * </ul>
 *
 * @param editionYear the Cannes edition year
 * @param section     the section name, or {@code null}
 * @param awardName   the award name, or {@code null} for a selection
 */
public record CannesArchiveEntry(int editionYear, String section, String awardName) {
}
