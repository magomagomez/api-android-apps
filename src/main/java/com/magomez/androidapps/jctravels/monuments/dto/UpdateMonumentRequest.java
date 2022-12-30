package com.magomez.androidapps.jctravels.monuments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record UpdateMonumentRequest(
        @JsonProperty("done")
        Boolean done,
        @JsonProperty("route")
        Integer route
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
