package com.magomez.androidapps.jcfit.training;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/** Body of POST /sessions. */
public record StartSessionRequest(
        Integer programDayId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date
) {
}
