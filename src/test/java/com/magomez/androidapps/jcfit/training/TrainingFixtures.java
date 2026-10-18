package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fake rows with the DAOs' column names, fixed clocks and error assertions. */
final class TrainingFixtures {

    /** Wednesday 2026-10-14 in Madrid; the program started on Tuesday 2026-09-01 (week 1 = Aug 31). */
    static final LocalDate PROGRAM_START = LocalDate.of(2026, 9, 1);
    static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 14);
    static final Clock WEDNESDAY_NOON = utcClock("2026-10-14T10:00:00Z");

    static final String ACTIVE_PROGRAM = "FROM jcfit_programs WHERE active";
    static final String DAYS_OF_PROGRAM = "FROM jcfit_program_days WHERE program_id = ?";
    static final String ACTIVE_DAY = "WHERE d.id = ? AND p.active";
    static final String PROGRAM_EXERCISES = "WHERE d.program_id = ?";
    static final String DAY_EXERCISES = "WHERE pe.program_day_id = ?";
    static final String IS_IN_DAY = "FROM jcfit_program_exercises WHERE program_day_id = ? AND exercise_id = ?";
    static final String EXERCISE_NAME = "FROM jcfit_exercises WHERE id = ?";
    static final String SESSIONS_BETWEEN = "WHERE session_date BETWEEN ? AND ?";
    static final String SESSION_OF_DAY = "WHERE program_day_id = ? AND session_date = ?";
    static final String SESSION_BY_ID = "FROM jcfit_workout_sessions WHERE id = ?";
    static final String INSERT_SESSION = "INSERT INTO jcfit_workout_sessions";
    static final String FINISH = "UPDATE jcfit_workout_sessions SET finished_at = COALESCE(finished_at, ?)";
    static final String LAST_DATE = "SELECT MAX(session_date) FROM jcfit_workout_sessions";
    static final String COMPLETED_COUNTS = "AS completed_sets";
    static final String SESSION_SETS = "WHERE l.session_id = ?";
    static final String PREVIOUS_SETS = "DISTINCT ON (l.exercise_id)";
    static final String HISTORY_SETS = "WHERE l.exercise_id = ? AND l.completed";
    static final String UPSERT_SET = "INSERT INTO jcfit_set_logs";
    static final String DELETE_SET = "DELETE FROM jcfit_set_logs";

    private TrainingFixtures() {
    }

    static Clock utcClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    static OffsetDateTime at(String instant) {
        return OffsetDateTime.parse(instant);
    }

    /** Unlike Map.of it accepts null values (SQL NULL). */
    static Map<String, Object> row(Object... columnsAndValues) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < columnsAndValues.length; i += 2) {
            row.put((String) columnsAndValues[i], columnsAndValues[i + 1]);
        }
        return row;
    }

    static Map<String, Object> program(int id, LocalDate startDate) {
        return row("id", id, "start_date", startDate);
    }

    static Map<String, Object> day(int id, int dayOfWeek, String name) {
        return row("id", id, "day_of_week", dayOfWeek, "name", name, "muscles", name + " muscles");
    }

    static Map<String, Object> activeDay(int id, int dayOfWeek, String name) {
        Map<String, Object> row = day(id, dayOfWeek, name);
        row.put("start_date", PROGRAM_START);
        return row;
    }

    static Map<String, Object> exercise(int id, int dayId, int exerciseId, int position, int sets) {
        return row("id", id, "program_day_id", dayId, "exercise_id", exerciseId, "position", position,
                "name", "Ejercicio " + exerciseId, "youtube_url", "https://youtu.be/dQw4w9WgXcQ",
                "instructions", null, "sets", sets, "reps_min", 8, "reps_max", 10, "rest_seconds", 90);
    }

    static Map<String, Object> session(int id, Integer dayId, LocalDate date, String finishedAt) {
        return row("id", id, "program_day_id", dayId, "day_name", "Pierna", "session_date", date,
                "started_at", at("2026-10-14T17:40:00Z"), "finished_at", finishedAt == null ? null : at(finishedAt));
    }

    static Map<String, Object> set(int sessionId, LocalDate date, int exerciseId, int setNumber, String weightKg,
                                   Integer reps, boolean completed) {
        return row("session_id", sessionId, "session_date", date, "day_name", "Pierna", "exercise_id", exerciseId,
                "set_number", setNumber, "weight_kg", weightKg == null ? null : new BigDecimal(weightKg),
                "reps", reps, "completed", completed);
    }

    static Map<String, Object> count(int sessionId, int exerciseId, long completedSets) {
        return row("session_id", sessionId, "exercise_id", exerciseId, "completed_sets", completedSets);
    }

    static void assertApiError(ThrowingCallable call, HttpStatus status, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            assertThat(e.getCode()).isEqualTo(code);
        });
    }
}
