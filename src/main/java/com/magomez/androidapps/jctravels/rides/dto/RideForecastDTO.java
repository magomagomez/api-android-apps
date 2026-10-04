package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A ride's expected waits for today (every 15 minutes, with their margin) and its usual wait
 * by day of the week (0 = Sunday). Both are empty when the source knows nothing.
 */
public record RideForecastDTO(
        @JsonProperty("points") List<Point> points,
        @JsonProperty("typical") List<TypicalDay> typical,
        @JsonProperty("timezone") String timezone,
        @JsonProperty("source") String source) {

    public record Point(
            @JsonProperty("time") String time,
            @JsonProperty("wait") Integer minutes,
            @JsonProperty("uncertainty") Integer uncertainty) {
    }

    public record TypicalDay(
            @JsonProperty("day_of_week") Integer dayOfWeek,
            @JsonProperty("typical") Integer typical,
            @JsonProperty("busy") Integer busy) {
    }
}
