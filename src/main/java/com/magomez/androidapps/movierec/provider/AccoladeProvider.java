package com.magomez.androidapps.movierec.provider;

import com.magomez.androidapps.movierec.model.AccoladeReport;
import com.magomez.androidapps.movierec.model.Movie;

/**
 * Abstraction over a source of a movie's recognition — awards tallies, festival
 * selections, festival awards.
 *
 * <p>Sibling of {@link MovieDataProvider} / {@link FestivalDataProvider} /
 * {@link CriticalReceptionProvider}: an external source that speaks only domain language
 * ({@link Movie} in, {@link AccoladeReport} out — no HTTP, no DTOs, no HTML, nothing
 * site-specific).
 *
 * <p>This only reports what a source knows. No {@code accoladeScore}, no ranking.
 */
public interface AccoladeProvider extends ExternalDataProvider {

    /**
     * @return what this source knows about {@code movie}'s recognition;
     *         {@link AccoladeReport#empty()} when it knows nothing (or nothing is
     *         connected yet). Implementations must not throw for a plain "not found".
     */
    AccoladeReport accoladesOf(Movie movie);
}
