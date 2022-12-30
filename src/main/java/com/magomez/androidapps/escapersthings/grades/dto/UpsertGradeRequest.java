package com.magomez.androidapps.escapersthings.grades.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record UpsertGradeRequest(
        @JsonProperty("nombre")
        String nombre,
        @JsonProperty("grades")
        GradeDTO grades
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
