package com.magomez.androidapps.movierec.provider.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieDetails(
        @JsonProperty("id") Integer id,
        @JsonProperty("imdb_id") String imdbId,
        @JsonProperty("title") String title,
        @JsonProperty("original_title") String originalTitle,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("runtime") Integer runtime,
        @JsonProperty("overview") String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("genres") List<TmdbGenre> genres,
        @JsonProperty("production_countries") List<TmdbProductionCountry> productionCountries,
        @JsonProperty("credits") TmdbCredits credits,
        @JsonProperty("similar") TmdbSimilar similar,
        @JsonProperty("vote_average") Double voteAverage,
        @JsonProperty("vote_count") Integer voteCount) {

    /** Backward-compatible constructor without {@code similar} (defaults to {@code null}). */
    public TmdbMovieDetails(Integer id, String imdbId, String title, String originalTitle,
                            String releaseDate, Integer runtime, String overview, String posterPath,
                            List<TmdbGenre> genres, List<TmdbProductionCountry> productionCountries,
                            TmdbCredits credits, Double voteAverage, Integer voteCount) {
        this(id, imdbId, title, originalTitle, releaseDate, runtime, overview, posterPath, genres,
                productionCountries, credits, null, voteAverage, voteCount);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbSimilar(@JsonProperty("results") List<TmdbSimilarResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbSimilarResult(
            @JsonProperty("id") Integer id,
            @JsonProperty("title") String title,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("genre_ids") List<Integer> genreIds) {

        public TmdbSimilarResult(Integer id, String title, String releaseDate) {
            this(id, title, releaseDate, List.of());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbGenre(
            @JsonProperty("id") Integer id,
            @JsonProperty("name") String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbProductionCountry(
            @JsonProperty("iso_3166_1") String code,
            @JsonProperty("name") String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbCredits(
            @JsonProperty("cast") List<TmdbCast> cast,
            @JsonProperty("crew") List<TmdbCrew> crew) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbCast(
            @JsonProperty("id") Integer id,
            @JsonProperty("name") String name,
            @JsonProperty("order") Integer order) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbCrew(
            @JsonProperty("id") Integer id,
            @JsonProperty("name") String name,
            @JsonProperty("job") String job) {
    }
}
