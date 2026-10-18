package com.magomez.androidapps.jcfit.admin;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The weekly program: jcfit_programs (one active), its days and the exercises of each day. */
@Repository
public class AdminProgramDao {

    private static final String ACTIVE = "SELECT id, name, start_date FROM jcfit_programs WHERE active";
    private static final String INSERT_ACTIVE =
            "INSERT INTO jcfit_programs (name, start_date, active) VALUES (?, ?, true) RETURNING id";
    private static final String UPDATE = "UPDATE jcfit_programs SET name = ?, start_date = ? WHERE id = ?";
    private static final String DAYS =
            "SELECT id, day_of_week, name, muscles FROM jcfit_program_days WHERE program_id = ? ORDER BY day_of_week";
    private static final String LINES = """
            SELECT pe.id, pe.program_day_id, pe.exercise_id, e.name AS exercise_name, e.archived,
                   pe.position, pe.sets, pe.reps_min, pe.reps_max, pe.rest_seconds
            FROM jcfit_program_exercises pe
            JOIN jcfit_program_days d ON d.id = pe.program_day_id
            JOIN jcfit_exercises e ON e.id = pe.exercise_id
            WHERE d.program_id = ?
            ORDER BY pe.program_day_id, pe.position
            """;
    /** Keeps the id of an existing day, so sessions already linked to it stay linked. */
    private static final String UPSERT_DAY = """
            INSERT INTO jcfit_program_days (program_id, day_of_week, name, muscles) VALUES (?, ?, ?, ?)
            ON CONFLICT (program_id, day_of_week) DO UPDATE SET name = EXCLUDED.name, muscles = EXCLUDED.muscles
            RETURNING id
            """;
    private static final String DELETE_LINES = "DELETE FROM jcfit_program_exercises WHERE program_day_id = ?";
    private static final String INSERT_LINE = """
            INSERT INTO jcfit_program_exercises
                (program_day_id, exercise_id, position, sets, reps_min, reps_max, rest_seconds)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String DELETE_DAY = "DELETE FROM jcfit_program_days WHERE program_id = ? AND day_of_week = ?";

    private static final RowMapper<AdminProgramRows.Program> PROGRAM = (rs, rowNum) -> new AdminProgramRows.Program(
            rs.getInt("id"), rs.getString("name"), rs.getObject("start_date", LocalDate.class));
    private static final RowMapper<AdminProgramRows.Day> DAY = (rs, rowNum) -> new AdminProgramRows.Day(
            rs.getInt("id"), rs.getInt("day_of_week"), rs.getString("name"), rs.getString("muscles"));
    private static final RowMapper<AdminProgramRows.Line> LINE = (rs, rowNum) -> new AdminProgramRows.Line(
            rs.getInt("program_day_id"),
            new AdminProgramExercise(rs.getInt("id"), rs.getInt("exercise_id"), rs.getString("exercise_name"),
                    rs.getBoolean("archived"), rs.getInt("position"), rs.getInt("sets"), rs.getInt("reps_min"),
                    rs.getInt("reps_max"), rs.getObject("rest_seconds", Integer.class)));

    private final JdbcTemplate jdbcTemplate;

    public AdminProgramDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<AdminProgramRows.Program> findActive() {
        return jdbcTemplate.query(ACTIVE, PROGRAM).stream().findFirst();
    }

    int insertActive(String name, LocalDate startDate) {
        Integer id = jdbcTemplate.queryForObject(INSERT_ACTIVE, Integer.class, name, startDate);
        return id == null ? 0 : id;
    }

    void update(int id, String name, LocalDate startDate) {
        jdbcTemplate.update(UPDATE, name, startDate, id);
    }

    List<AdminProgramRows.Day> findDays(int programId) {
        return jdbcTemplate.query(DAYS, DAY, programId);
    }

    List<AdminProgramRows.Line> findLines(int programId) {
        return jdbcTemplate.query(LINES, LINE, programId);
    }

    /** The id of the day, new or kept. */
    int upsertDay(int programId, int dayOfWeek, String name, String muscles) {
        Integer id = jdbcTemplate.queryForObject(UPSERT_DAY, Integer.class, programId, dayOfWeek, name, muscles);
        return id == null ? 0 : id;
    }

    /** Replaces every line of the day; positions are 1..n in list order. */
    void replaceLines(int programDayId, List<AdminProgramExerciseRequest> lines) {
        jdbcTemplate.update(DELETE_LINES, programDayId);
        List<Object[]> batch = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            AdminProgramExerciseRequest line = lines.get(i);
            batch.add(new Object[]{programDayId, line.exerciseId(), i + 1, line.sets(), line.repsMin(), line.repsMax(),
                    line.restSeconds()});
        }
        jdbcTemplate.batchUpdate(INSERT_LINE, batch);
    }

    void deleteDay(int programId, int dayOfWeek) {
        jdbcTemplate.update(DELETE_DAY, programId, dayOfWeek);
    }
}
