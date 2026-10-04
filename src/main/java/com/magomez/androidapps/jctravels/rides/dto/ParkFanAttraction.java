package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Body of a park.fan attraction (only the forecast, the typical waits and the park's time zone). */
public record ParkFanAttraction(
        @JsonProperty("park") Park park,
        @JsonProperty("hourlyForecast") List<Prediction> hourlyForecast,
        @JsonProperty("typicalWaits") TypicalWaits typicalWaits) {

    public record Park(@JsonProperty("timezone") String timezone) {
    }

    public record Prediction(
            @JsonProperty("predictedTime") String predictedTime,
            @JsonProperty("predictedWaitTime") Integer predictedWaitTime,
            @JsonProperty("uncertaintyMinutes") Integer uncertaintyMinutes) {
    }

    public record TypicalWaits(@JsonProperty("byDayOfWeek") List<Day> byDayOfWeek) {
    }

    public record Day(
            @JsonProperty("dayOfWeek") Integer dayOfWeek,
            @JsonProperty("typical") Integer typical,
            @JsonProperty("busy") Integer busy) {
    }
}
