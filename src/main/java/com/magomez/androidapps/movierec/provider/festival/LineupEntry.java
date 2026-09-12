package com.magomez.androidapps.movierec.provider.festival;

/**
 * One film in a festival edition's programme, as a {@link FestivalLineupSource} extracts
 * it before it is matched against a candidate. Provider-internal.
 *
 * @param title   the film title exactly as the source lists it (never blank)
 * @param section the programme section it played in, e.g. {@code "Midnight"} /
 *                {@code "Competition"}; {@code null} when the source does not say
 */
public record LineupEntry(String title, String section) {
}
