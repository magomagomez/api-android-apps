package com.magomez.androidapps.jctravels.parks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

/** The park's current or next opening: park-local date, times with offset, and Early Entry when there is one. */
public record ParkHoursDTO(
        @JsonProperty("date")
        String date,
        @JsonProperty("opening")
        String opening,
        @JsonProperty("closing")
        String closing,
        @JsonProperty("early_entry")
        String earlyEntry
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
