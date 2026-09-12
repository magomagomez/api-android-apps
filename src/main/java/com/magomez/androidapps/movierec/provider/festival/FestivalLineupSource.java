package com.magomez.androidapps.movierec.provider.festival;

import java.io.IOException;
import java.util.Optional;

/**
 * Where {@link FestivalLineupAccoladeProvider} gets the films of a festival edition.
 * Kept separate from the provider so the HTTP / HTML work is isolated and swappable, and
 * the matching logic can be tested with fakes.
 */
public interface FestivalLineupSource {

    /**
     * @return the lineup of {@code festival}'s {@code editionYear} edition, or
     *         {@link Optional#empty()} when this source has no page for it
     * @throws IOException on a transport failure
     */
    Optional<FestivalLineup> lineup(String festival, int editionYear) throws IOException;
}
