package com.magomez.androidapps.movierec.model;

/**
 * A production country. Domain value object: temporary, immutable and free of any
 * transport, serialization or provider detail.
 *
 * <p>Structured on purpose so later phases can use the ISO code to cross data between
 * providers and to compute country affinity.
 *
 * @param code ISO 3166-1 code when known (e.g. {@code "FR"}), otherwise {@code null}
 * @param name human readable country name
 */
public record Country(String code, String name) {

    public Country {
        code = code == null ? null : code.trim();
        name = name == null ? null : name.trim();
    }

    public static Country of(String name) {
        return new Country(null, name);
    }
}
