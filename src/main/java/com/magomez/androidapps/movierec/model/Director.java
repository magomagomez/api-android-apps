package com.magomez.androidapps.movierec.model;

/**
 * A film director. Domain value object: temporary, immutable and free of any
 * transport, serialization or provider detail.
 *
 * <p>Kept as its own concept (separate from {@link Actor}) so later phases can build
 * director-based relations and the director-affinity signal. The external person id is
 * carried to allow cross-referencing between providers.
 *
 * @param tmdbId external person id when known, otherwise {@code null}
 * @param name   director full name
 */
public record Director(Integer tmdbId, String name) {

    public Director {
        name = name == null ? null : name.trim();
    }

    public static Director of(String name) {
        return new Director(null, name);
    }
}
