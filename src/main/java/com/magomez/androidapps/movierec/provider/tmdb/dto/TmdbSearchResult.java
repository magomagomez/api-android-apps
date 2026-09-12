package com.magomez.androidapps.movierec.provider.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbSearchResult(
        @JsonProperty("id") Integer id,
        @JsonProperty("title") String title,
        @JsonProperty("original_title") String originalTitle,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("vote_count") Integer voteCount) {

    /** Audience vote count, or {@code 0} when TMDB did not report one. */
    public int voteCountOrZero() {
        return voteCount == null ? 0 : voteCount;
    }

    /** Release year parsed from {@code release_date} ("YYYY-MM-DD"), or {@code null}. */
    public Integer releaseYear() {
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
