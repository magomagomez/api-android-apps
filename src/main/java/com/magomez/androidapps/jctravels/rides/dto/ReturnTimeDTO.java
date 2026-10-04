package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

/** Lightning Lane for the app: state (AVAILABLE, TEMP_FULL, FINISHED), window with offset, and price when paid. */
public record ReturnTimeDTO(
        @JsonProperty("state")
        String state,
        @JsonProperty("start")
        String start,
        @JsonProperty("end")
        String end,
        @JsonProperty("price")
        String price
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
