package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Body of park.fan /v1/search (only what finding a ride needs). */
public record ParkFanSearch(@JsonProperty("results") List<Result> results) {

    public record Result(
            @JsonProperty("type") String type,
            @JsonProperty("name") String name,
            @JsonProperty("url") String url,
            @JsonProperty("parentPark") ParentPark parentPark) {
    }

    public record ParentPark(@JsonProperty("name") String name) {
    }
}
