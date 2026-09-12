package com.magomez.androidapps.movierec.model;

/**
 * A film genre. Domain value object: temporary, immutable and free of any transport,
 * serialization or provider detail.
 *
 * <p>Structured on purpose so later phases can attach it to the cinematographic graph
 * and to recommendation signals (genre affinity). How it is exposed over HTTP is the
 * responsibility of the API layer, not of this model.
 *
 * @param tmdbId external genre id when known, otherwise {@code null}
 * @param name   human readable genre name
 */
public record Genre(Integer tmdbId, String name) {

    public Genre {
        name = name == null ? null : name.trim();
    }

    public static Genre of(String name) {
        return new Genre(null, name);
    }
}
