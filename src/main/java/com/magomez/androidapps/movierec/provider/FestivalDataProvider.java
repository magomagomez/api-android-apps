package com.magomez.androidapps.movierec.provider;

import com.magomez.androidapps.movierec.model.FestivalAchievement;
import com.magomez.androidapps.movierec.model.Movie;

import java.util.List;

/**
 * Abstraction over a film festival's historical information.
 *
 * <p>Sibling of {@link MovieDataProvider} / {@link MovieEnricher}: an external source
 * that speaks only domain language ({@link Movie} in, {@link FestivalAchievement}s out —
 * no HTTP, no DTOs). Concrete festivals live in their own sub-package (e.g.
 * {@code provider.cannes}).
 *
 * <p>This only reports what a festival's records say about a movie. No
 * {@code festivalScore}, no ranking.
 */
public interface FestivalDataProvider extends ExternalDataProvider {

    /**
     * @return the festival achievements known for {@code movie} (selections and/or
     *         awards), in no particular order; empty when the festival has no record of
     *         it — or when nothing is connected yet
     */
    List<FestivalAchievement> achievementsOf(Movie movie);
}
