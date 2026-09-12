package com.magomez.androidapps.movierec.provider;

import com.magomez.androidapps.movierec.model.CriticalReception;
import com.magomez.androidapps.movierec.model.Movie;

import java.util.List;

/**
 * Abstraction over a source of critical-reception data for movies.
 *
 * <p>Sibling of {@link MovieDataProvider} / {@link FestivalDataProvider}: an external
 * source that speaks only domain language ({@link Movie} in, {@link CriticalReception}s
 * out — no HTTP, no DTOs, no HTML, nothing site-specific).
 *
 * <p>This only reports what a source observed. No scoring, no normalization to a common
 * scale, no ranking.
 */
public interface CriticalReceptionProvider extends ExternalDataProvider {

    /**
     * @return the critical-reception observations known for {@code movie}, in no
     *         particular order; empty when nothing is known
     */
    List<CriticalReception> receptionOf(Movie movie);
}
