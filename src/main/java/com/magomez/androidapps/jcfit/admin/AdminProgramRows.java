package com.magomez.androidapps.jcfit.admin;

import java.time.LocalDate;

/** Rows of the program tables as {@link AdminProgramDao} reads them. */
final class AdminProgramRows {

    private AdminProgramRows() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    record Program(int id, String name, LocalDate startDate) {
    }

    record Day(int id, int dayOfWeek, String name, String muscles) {
    }

    record Line(int programDayId, AdminProgramExercise exercise) {
    }
}
