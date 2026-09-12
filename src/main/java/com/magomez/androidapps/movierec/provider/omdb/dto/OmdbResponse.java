package com.magomez.androidapps.movierec.provider.omdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw OMDb "by IMDb ID" payload. Confined to {@code provider/}: it never leaves the
 * OMDb package.
 *
 * <p>OMDb sends every value as a string and uses {@code "N/A"} for missing data;
 * parsing is left to {@code OmdbRatingMapper}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbResponse(
        @JsonProperty("Response") String response,
        @JsonProperty("Error") String error,
        @JsonProperty("imdbRating") String imdbRating,
        @JsonProperty("imdbVotes") String imdbVotes,
        @JsonProperty("Metascore") String metascore,
        @JsonProperty("Awards") String awards,
        @JsonProperty("Ratings") List<OmdbRating> ratings) {

    /** Back-compat constructor for callers that predate the {@code Awards} field. */
    public OmdbResponse(String response, String error, String imdbRating, String imdbVotes,
                        String metascore, List<OmdbRating> ratings) {
        this(response, error, imdbRating, imdbVotes, metascore, null, ratings);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OmdbRating(
            @JsonProperty("Source") String source,
            @JsonProperty("Value") String value) {
    }

    /** OMDb reports lookup success in a {@code "Response": "True"|"False"} field. */
    public boolean isSuccess() {
        return "True".equalsIgnoreCase(response);
    }

    public List<OmdbRating> ratingsOrEmpty() {
        return ratings == null ? List.of() : ratings;
    }
}
