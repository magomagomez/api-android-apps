package com.magomez.androidapps.movierec.provider;

import com.magomez.androidapps.movierec.model.MovieMatch;
import com.magomez.androidapps.movierec.model.MovieQuery;

/**
 * Abstraction over an external source of cinematographic identification data.
 *
 * <p>Speaks only the domain language ({@link MovieQuery} in, {@link MovieMatch} out):
 * no HTTP, no DTOs, nothing TMDB-specific. Implementations are replaceable and
 * mockable, and the domain never depends on a concrete provider.
 *
 * <p>Enriching an identified movie with extra data (ratings, ...) is a separate
 * concern handled by {@link MovieEnricher}.
 */
public interface MovieDataProvider extends ExternalDataProvider {

    /**
     * Try to identify the given query against the external source.
     *
     * @throws MovieProviderException when the external source cannot be reached or
     *                                returns an unusable response
     */
    MovieMatch identify(MovieQuery query) throws MovieProviderException;
}
