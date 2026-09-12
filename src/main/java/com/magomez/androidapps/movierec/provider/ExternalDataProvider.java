package com.magomez.androidapps.movierec.provider;

/**
 * Common contract for every external data source the recommendation engine reads from.
 *
 * <p>Today the only kind is {@link MovieDataProvider}; later phases will add sibling
 * contracts (ratings, awards, festivals, critics) that also extend this interface.
 *
 * <p>Implementations live in their own sub-package (e.g. {@code provider.tmdb}) and
 * must never expose their transport, DTOs or vendor specifics to the domain
 * ({@code model/}) or to {@code service/}.
 */
public interface ExternalDataProvider {

    /** Stable identifier of the backing source, e.g. {@code "TMDB"}. Used for diagnostics. */
    String sourceName();
}
