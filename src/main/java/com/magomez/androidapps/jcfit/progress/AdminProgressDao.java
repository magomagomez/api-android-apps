package com.magomez.androidapps.jcfit.progress;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Reads Cris's latest sessions with their completed sets for the backoffice, in one query. */
@Repository
public class AdminProgressDao {

    /** Sets in the order they were first saved (l.id), so exercises come in the order they were done. */
    private static final String RECENT_SESSIONS = """
            WITH recent AS (
                SELECT id, session_date, day_name, finished_at
                FROM jcfit_workout_sessions
                ORDER BY session_date DESC, id DESC LIMIT ?
            )
            SELECT r.id AS session_id, r.session_date, r.day_name, r.finished_at IS NOT NULL AS finished,
                   l.exercise_id, e.name AS exercise_name, l.set_number, l.weight_kg, l.reps
            FROM recent r
            LEFT JOIN jcfit_set_logs l ON l.session_id = r.id AND l.completed
            LEFT JOIN jcfit_exercises e ON e.id = l.exercise_id
            ORDER BY r.session_date DESC, r.id DESC, l.id
            """;

    private static final RowMapper<AdminProgressRow> ROW = (rs, rowNum) -> new AdminProgressRow(
            rs.getInt("session_id"), rs.getObject("session_date", LocalDate.class), rs.getString("day_name"),
            rs.getBoolean("finished"), rs.getObject("exercise_id", Integer.class), rs.getString("exercise_name"),
            rs.getObject("set_number", Integer.class), plain(rs.getBigDecimal("weight_kg")),
            rs.getObject("reps", Integer.class));

    private final JdbcTemplate jdbcTemplate;

    public AdminProgressDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<AdminProgressRow> findRecentSessions(int limit) {
        return jdbcTemplate.query(RECENT_SESSIONS, ROW, limit);
    }

    /** numeric(6,2) reads as 62.50: the backoffice gets 62.5. */
    private static BigDecimal plain(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
