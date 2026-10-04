package com.magomez.androidapps.jctravels.parks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

/** One period of a themeparks.wiki /schedule: OPERATING, or TICKETED_EVENT such as "Early Entry". Times carry the park's offset. */
public record ScheduleEntry(
        @JsonProperty("date")
        String date,
        @JsonProperty("type")
        String type,
        @JsonProperty("description")
        String description,
        @JsonProperty("openingTime")
        String openingTime,
        @JsonProperty("closingTime")
        String closingTime
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
