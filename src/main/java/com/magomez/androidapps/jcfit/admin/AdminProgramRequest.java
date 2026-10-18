package com.magomez.androidapps.jcfit.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/** Body of PUT /admin/program. */
public record AdminProgramRequest(
        String name,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate) {
}
