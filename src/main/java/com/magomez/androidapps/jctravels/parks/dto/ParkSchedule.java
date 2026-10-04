package com.magomez.androidapps.jctravels.parks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Body of themeparks.wiki /v1/entity/{id}/schedule (only what the hours need). */
public record ParkSchedule(
        @JsonProperty("schedule")
        List<ScheduleEntry> schedule
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
