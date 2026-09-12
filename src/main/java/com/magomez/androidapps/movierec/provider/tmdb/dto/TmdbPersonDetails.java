package com.magomez.androidapps.movierec.provider.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * TMDB {@code GET /person/{id}} payload — only the bit we use: the primary name and the
 * {@code also_known_as} spellings, so a name credited only in a non-Latin script can be
 * shown romanized.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbPersonDetails(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("also_known_as") List<String> alsoKnownAs) {

    public TmdbPersonDetails {
        alsoKnownAs = alsoKnownAs == null ? List.of() : List.copyOf(alsoKnownAs);
    }
}
