package com.magomez.androidapps.jcfit.training;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Rows read by the training DAOs, before they are put together into the API shapes. */
final class TrainingRows {

    private TrainingRows() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    record Program(int id, LocalDate startDate) {
    }

    /** jcfit_program_days: dayOfWeek 1 = Monday ... 7 = Sunday. */
    record Day(int id, int dayOfWeek, String name, String muscles) {
    }

    /** A day of the active program with the program's start date. */
    record ActiveDay(Day day, LocalDate programStart) {
    }

    /** jcfit_program_exercises joined with jcfit_exercises; id is the program exercise. */
    record PlannedExercise(int id, int programDayId, int exerciseId, int position, String name, String youtubeUrl,
                           String instructions, int sets, int repsMin, int repsMax, Integer restSeconds) {
    }

    /** jcfit_workout_sessions; programDayId is null once the day is removed from the program. */
    record Session(int id, Integer programDayId, String dayName, LocalDate date, Instant startedAt,
                   Instant finishedAt) {
    }

    record CompletedCount(int sessionId, int exerciseId, int completedSets) {
    }

    /** jcfit_set_logs with the date and day name of its session. */
    record SetRow(int sessionId, LocalDate date, String dayName, int exerciseId, int setNumber, BigDecimal weightKg,
                  Integer reps, boolean completed) {
    }
}
