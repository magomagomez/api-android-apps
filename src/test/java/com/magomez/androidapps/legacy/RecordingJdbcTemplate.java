package com.magomez.androidapps.legacy;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

/**
 * Hand-written {@link JdbcTemplate} stand-in (no Mockito, same reason as the movierec
 * tests: the CI JVM cannot always instrument concrete classes). Records the last SQL
 * and bind arguments so a test can assert that request values travel as parameters,
 * never inside the SQL text. No database is touched.
 */
class RecordingJdbcTemplate extends JdbcTemplate {

    String lastSql;
    Object[] lastArgs;
    Object nextResult;
    DataAccessException nextFailure;

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        record(sql, args);
        return List.of();
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        return answer(sql, args);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
        return answer(sql, args);
    }

    @Override
    public int update(String sql, Object... args) {
        record(sql, args);
        return 1;
    }

    /** SQL with runs of whitespace collapsed, so assertions don't depend on spacing. */
    String normalizedSql() {
        return lastSql.replaceAll("\\s+", " ").trim();
    }

    @SuppressWarnings("unchecked")
    private <T> T answer(String sql, Object[] args) {
        record(sql, args);
        if (nextFailure != null) {
            throw nextFailure;
        }
        return (T) nextResult;
    }

    private void record(String sql, Object[] args) {
        this.lastSql = sql;
        this.lastArgs = args;
    }
}
