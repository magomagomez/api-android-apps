package com.magomez.androidapps.movierec.provider.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbSearchResponse(@JsonProperty("results") List<TmdbSearchResult> results) {

    public List<TmdbSearchResult> resultsOrEmpty() {
        return results == null ? List.of() : results;
    }
}
