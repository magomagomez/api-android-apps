package com.magomez.androidapps.jcfit.training;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Reads the active program for the app; the admin writes it (jcfit_programs, _days, _exercises). */
@Repository
public class TrainingProgramDao {

    private static final String ACTIVE_PROGRAM = "SELECT id, start_date FROM jcfit_programs WHERE active";
    private static final String DAYS_OF_PROGRAM =
            "SELECT id, day_of_week, name, muscles FROM jcfit_program_days WHERE program_id = ? ORDER BY day_of_week";
    private static final String ACTIVE_DAY = """
            SELECT d.id, d.day_of_week, d.name, d.muscles, p.start_date
            FROM jcfit_program_days d
            JOIN jcfit_programs p ON p.id = d.program_id
            WHERE d.id = ? AND p.active
            """;
    private static final String SELECT_EXERCISES = """
            SELECT pe.id, pe.program_day_id, pe.exercise_id, pe.position, e.name, e.youtube_url, e.instructions,
                   pe.sets, pe.reps_min, pe.reps_max, pe.rest_seconds
            FROM jcfit_program_exercises pe
            JOIN jcfit_exercises e ON e.id = pe.exercise_id
            """;
    private static final String EXERCISES_OF_PROGRAM = SELECT_EXERCISES + """
            JOIN jcfit_program_days d ON d.id = pe.program_day_id
            WHERE d.program_id = ?
            ORDER BY pe.program_day_id, pe.position
            """;
    private static final String EXERCISES_OF_DAY = SELECT_EXERCISES + " WHERE pe.program_day_id = ? ORDER BY pe.position";
    private static final String IS_IN_DAY =
            "SELECT COUNT(*) FROM jcfit_program_exercises WHERE program_day_id = ? AND exercise_id = ?";
    /** Archived exercises too: they keep their history. */
    private static final String EXERCISE_NAME = "SELECT name FROM jcfit_exercises WHERE id = ?";

    private static final RowMapper<TrainingRows.Program> PROGRAM = (rs, rowNum) -> new TrainingRows.Program(
            rs.getInt("id"), rs.getObject("start_date", LocalDate.class));
    private static final RowMapper<TrainingRows.Day> DAY = (rs, rowNum) -> new TrainingRows.Day(
            rs.getInt("id"), rs.getInt("day_of_week"), rs.getString("name"), rs.getString("muscles"));
    private static final RowMapper<TrainingRows.ActiveDay> ACTIVE_DAY_ROW = (rs, rowNum) -> new TrainingRows.ActiveDay(
            DAY.mapRow(rs, rowNum), rs.getObject("start_date", LocalDate.class));
    private static final RowMapper<TrainingRows.PlannedExercise> EXERCISE = (rs, rowNum) ->
            new TrainingRows.PlannedExercise(rs.getInt("id"), rs.getInt("program_day_id"), rs.getInt("exercise_id"),
                    rs.getInt("position"), rs.getString("name"), rs.getString("youtube_url"),
                    rs.getString("instructions"), rs.getInt("sets"), rs.getInt("reps_min"), rs.getInt("reps_max"),
                    rs.getObject("rest_seconds", Integer.class));

    private final JdbcTemplate jdbcTemplate;

    public TrainingProgramDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<TrainingRows.Program> findActive() {
        return jdbcTemplate.query(ACTIVE_PROGRAM, PROGRAM).stream().findFirst();
    }

    List<TrainingRows.Day> findDays(int programId) {
        return jdbcTemplate.query(DAYS_OF_PROGRAM, DAY, programId);
    }

    /** Empty when the day does not exist or is not of the active program. */
    Optional<TrainingRows.ActiveDay> findActiveDay(int programDayId) {
        return jdbcTemplate.query(ACTIVE_DAY, ACTIVE_DAY_ROW, programDayId).stream().findFirst();
    }

    /** Every exercise of every day of the program, in one query. */
    List<TrainingRows.PlannedExercise> findExercises(int programId) {
        return jdbcTemplate.query(EXERCISES_OF_PROGRAM, EXERCISE, programId);
    }

    List<TrainingRows.PlannedExercise> findDayExercises(int programDayId) {
        return jdbcTemplate.query(EXERCISES_OF_DAY, EXERCISE, programDayId);
    }

    boolean isInDay(int programDayId, int exerciseId) {
        Integer count = jdbcTemplate.queryForObject(IS_IN_DAY, Integer.class, programDayId, exerciseId);
        return count != null && count > 0;
    }

    Optional<String> findExerciseName(int exerciseId) {
        return jdbcTemplate.query(EXERCISE_NAME, (rs, rowNum) -> rs.getString("name"), exerciseId).stream()
                .findFirst();
    }
}
