package com.magomez.androidapps.restaurants;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written {@link JdbcTemplate} stand-in, like the one in the legacy tests: records the SQL
 * and bind arguments so a test can check that request values travel as parameters. No database.
 */
class RecordingJdbcTemplate extends JdbcTemplate {

    String lastSql;
    Object[] lastArgs;
    List<?> nextList = List.of();
    Object nextResult;
    DataAccessException nextFailure;
    int nextUpdateCount = 1;
    final List<String> history = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        record(sql, args);
        return (List<T>) nextList;
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper) {
        return query(sql, rowMapper, new Object[0]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        record(sql, args);
        if (nextFailure != null) {
            throw nextFailure;
        }
        return (T) nextResult;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
        record(sql, args);
        if (nextFailure != null) {
            throw nextFailure;
        }
        return (T) nextResult;
    }

    @Override
    public int update(String sql, Object... args) {
        record(sql, args);
        return nextUpdateCount;
    }

    /** SQL with runs of whitespace collapsed, so assertions don't depend on spacing. */
    String normalizedSql() {
        return lastSql.replaceAll("\\s+", " ").trim();
    }

    private void record(String sql, Object[] args) {
        history.add(sql);
        lastSql = sql;
        lastArgs = args;
    }
}
