package com.magomez.androidapps.escapersthings.users.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record LoginUserRequest(
        @JsonProperty("nombre")
        String nombre,
        @JsonProperty("password")
        String password
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
