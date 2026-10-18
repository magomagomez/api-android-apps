package com.magomez.androidapps.jcfit.training;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** jcfit_workout_sessions: one per program day and date. */
@Repository
public class WorkoutSessionDao {

    private static final String COLUMNS = "id, program_day_id, day_name, session_date, started_at, finished_at";
    private static final String SELECT = "SELECT " + COLUMNS + " FROM jcfit_workout_sessions";
    private static final String BETWEEN = SELECT + " WHERE session_date BETWEEN ? AND ? ORDER BY session_date, id";
    private static final String OF_DAY = SELECT + " WHERE program_day_id = ? AND session_date = ?";
    private static final String BY_ID = SELECT + " WHERE id = ?";
    /** Nothing comes back when the day and date already have a session. */
    private static final String INSERT = """
            INSERT INTO jcfit_workout_sessions (program_day_id, day_name, session_date, started_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (program_day_id, session_date) DO NOTHING
            RETURNING\s""" + COLUMNS;
    /** Finishing twice keeps the first time. */
    private static final String FINISH = "UPDATE jcfit_workout_sessions SET finished_at = COALESCE(finished_at, ?)"
            + " WHERE id = ? RETURNING " + COLUMNS;
    private static final String LAST_DATE =
            "SELECT MAX(session_date) FROM jcfit_workout_sessions WHERE program_day_id = ? AND session_date < ?";

    private static final RowMapper<TrainingRows.Session> SESSION = (rs, rowNum) -> new TrainingRows.Session(
            rs.getInt("id"), rs.getObject("program_day_id", Integer.class), rs.getString("day_name"),
            rs.getObject("session_date", LocalDate.class), instant(rs.getObject("started_at", OffsetDateTime.class)),
            instant(rs.getObject("finished_at", OffsetDateTime.class)));

    private final JdbcTemplate jdbcTemplate;

    public WorkoutSessionDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<TrainingRows.Session> findBetween(LocalDate from, LocalDate to) {
        return jdbcTemplate.query(BETWEEN, SESSION, from, to);
    }

    Optional<TrainingRows.Session> find(int programDayId, LocalDate date) {
        return jdbcTemplate.query(OF_DAY, SESSION, programDayId, date).stream().findFirst();
    }

    Optional<TrainingRows.Session> findById(int sessionId) {
        return jdbcTemplate.query(BY_ID, SESSION, sessionId).stream().findFirst();
    }

    /** Empty when that day and date already had a session. */
    Optional<TrainingRows.Session> insertIfAbsent(int programDayId, String dayName, LocalDate date,
                                                  OffsetDateTime startedAt) {
        return jdbcTemplate.query(INSERT, SESSION, programDayId, dayName, date, startedAt).stream().findFirst();
    }

    /** Empty when the session does not exist. */
    Optional<TrainingRows.Session> finish(int sessionId, OffsetDateTime finishedAt) {
        return jdbcTemplate.query(FINISH, SESSION, finishedAt, sessionId).stream().findFirst();
    }

    /** The latest session of the program day before date, or empty. */
    Optional<LocalDate> findLastDate(int programDayId, LocalDate before) {
        return Optional.ofNullable(jdbcTemplate.queryForObject(LAST_DATE, LocalDate.class, programDayId, before));
    }

    private static Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
