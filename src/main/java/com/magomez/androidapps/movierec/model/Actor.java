package com.magomez.androidapps.movierec.model;

/**
 * A cast member. Domain value object: temporary, immutable and free of any transport,
 * serialization or provider detail.
 *
 * <p>Kept as its own concept (separate from {@link Director}) so later phases can
 * build collaborator relations and the actor-affinity signal. The external person id
 * is carried to allow cross-referencing between providers.
 *
 * @param tmdbId external person id when known, otherwise {@code null}
 * @param name   actor full name
 */
public record Actor(Integer tmdbId, String name) {

    public Actor {
        name = name == null ? null : name.trim();
    }

    public static Actor of(String name) {
        return new Actor(null, name);
    }
}
