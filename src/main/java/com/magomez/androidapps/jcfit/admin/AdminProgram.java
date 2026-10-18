package com.magomez.androidapps.jcfit.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/** The active program; days by dayOfWeek, rest days left out. */
public record AdminProgram(
        int id,
        String name,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,
        List<AdminProgramDay> days) {
}
