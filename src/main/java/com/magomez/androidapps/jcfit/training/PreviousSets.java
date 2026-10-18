package com.magomez.androidapps.jcfit.training;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/** The completed sets of an exercise in the latest earlier session that has any. */
public record PreviousSets(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        List<SetEntry> sets
) {
}
