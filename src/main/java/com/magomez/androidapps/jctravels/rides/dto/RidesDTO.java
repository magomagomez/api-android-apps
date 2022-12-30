package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record RidesDTO(
        @JsonProperty("must")
        List<RideDTO> must,
        @JsonProperty("maybe")
        List<RideDTO> maybe
    ) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

}
