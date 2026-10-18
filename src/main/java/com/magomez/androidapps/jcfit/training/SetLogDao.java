package com.magomez.androidapps.jcfit.training;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** jcfit_set_logs: one row per session, exercise and set number. */
@Repository
public class SetLogDao {

    private static final String COLUMNS =
            "l.session_id, s.session_date, s.day_name, l.exercise_id, l.set_number, l.weight_kg, l.reps, l.completed";
    private static final String SELECT = "SELECT " + COLUMNS
            + " FROM jcfit_set_logs l JOIN jcfit_workout_sessions s ON s.id = l.session_id";
    private static final String OF_SESSION = SELECT + " WHERE l.session_id = ? ORDER BY l.exercise_id, l.set_number";
    private static final String COMPLETED_COUNTS = """
            SELECT l.session_id, l.exercise_id, COUNT(*) AS completed_sets
            FROM jcfit_set_logs l
            JOIN jcfit_workout_sessions s ON s.id = l.session_id
            WHERE l.completed AND s.session_date BETWEEN ? AND ?
            GROUP BY l.session_id, l.exercise_id
            """;
    /** For each exercise of the day, the completed sets of its latest session before the date. */
    private static final String PREVIOUS = """
            WITH latest AS (
                SELECT DISTINCT ON (l.exercise_id) l.exercise_id, l.session_id
                FROM jcfit_set_logs l
                JOIN jcfit_workout_sessions s ON s.id = l.session_id
                WHERE l.completed AND s.session_date < ?
                  AND l.exercise_id IN (SELECT exercise_id FROM jcfit_program_exercises WHERE program_day_id = ?)
                ORDER BY l.exercise_id, s.session_date DESC, s.id DESC
            )
            """ + SELECT + """
             JOIN latest ON latest.exercise_id = l.exercise_id AND latest.session_id = l.session_id
            WHERE l.completed
            ORDER BY l.exercise_id, l.set_number
            """;
    private static final String HISTORY = SELECT
            + " WHERE l.exercise_id = ? AND l.completed ORDER BY s.session_date DESC, s.id DESC, l.set_number";
    private static final String UPSERT = """
            INSERT INTO jcfit_set_logs (session_id, exercise_id, set_number, weight_kg, reps, completed, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (session_id, exercise_id, set_number) DO UPDATE
            SET weight_kg = EXCLUDED.weight_kg, reps = EXCLUDED.reps, completed = EXCLUDED.completed,
                updated_at = EXCLUDED.updated_at
            """;
    private static final String DELETE =
            "DELETE FROM jcfit_set_logs WHERE session_id = ? AND exercise_id = ? AND set_number = ?";

    private static final RowMapper<TrainingRows.SetRow> SET = (rs, rowNum) -> new TrainingRows.SetRow(
            rs.getInt("session_id"), rs.getObject("session_date", LocalDate.class), rs.getString("day_name"),
            rs.getInt("exercise_id"), rs.getInt("set_number"), rs.getBigDecimal("weight_kg"),
            rs.getObject("reps", Integer.class), rs.getBoolean("completed"));
    private static final RowMapper<TrainingRows.CompletedCount> COUNT = (rs, rowNum) ->
            new TrainingRows.CompletedCount(rs.getInt("session_id"), rs.getInt("exercise_id"),
                    rs.getInt("completed_sets"));

    private final JdbcTemplate jdbcTemplate;

    public SetLogDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<TrainingRows.SetRow> findBySession(int sessionId) {
        return jdbcTemplate.query(OF_SESSION, SET, sessionId);
    }

    /** Completed sets per session and exercise of the sessions between both dates. */
    List<TrainingRows.CompletedCount> countCompleted(LocalDate from, LocalDate to) {
        return jdbcTemplate.query(COMPLETED_COUNTS, COUNT, from, to);
    }

    List<TrainingRows.SetRow> findPrevious(int programDayId, LocalDate before) {
        return jdbcTemplate.query(PREVIOUS, SET, before, programDayId);
    }

    /** Every completed set of the exercise, newest session first. */
    List<TrainingRows.SetRow> findCompleted(int exerciseId) {
        return jdbcTemplate.query(HISTORY, SET, exerciseId);
    }

    void upsert(int sessionId, SaveSetRequest set, boolean completed, OffsetDateTime updatedAt) {
        jdbcTemplate.update(UPSERT, sessionId, set.exerciseId(), set.setNumber(), set.weightKg(), set.reps(),
                completed, updatedAt);
    }

    void delete(int sessionId, int exerciseId, int setNumber) {
        jdbcTemplate.update(DELETE, sessionId, exerciseId, setNumber);
    }
}
