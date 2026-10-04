package com.magomez.androidapps.jctravels.parks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record ParkDTO(
        @JsonProperty("id")
        Integer id,
        @JsonProperty("name")
        String name,
        @JsonProperty("queue_id")
        String queueId,
        @JsonProperty("city")
        Integer city,
        // Current or next opening; null when the schedule is unknown or could not be read.
        @JsonProperty("hours")
        ParkHoursDTO hours
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
