package com.magomez.androidapps.escapersthings.v2;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-written {@link JdbcTemplate} stand-in for the v2 DAOs (no Mockito, no database).
 * Records every statement with its bind arguments, and answers queries with rows chosen by a
 * fragment of their SQL. The DAO's own RowMapper runs over those rows through a fake
 * {@link ResultSet}, so a misspelt column name fails the test.
 */
class RecordingJdbcTemplate extends JdbcTemplate {

    final List<Call> calls = new ArrayList<>();
    private final Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
    private final Map<String, Object> scalars = new LinkedHashMap<>();
    private final Map<String, Integer> updateCounts = new LinkedHashMap<>();
    private final Map<String, DataAccessException> failures = new LinkedHashMap<>();

    @SafeVarargs
    final RecordingJdbcTemplate rows(String sqlFragment, Map<String, Object>... answer) {
        rows.put(sqlFragment, List.of(answer));
        return this;
    }

    RecordingJdbcTemplate scalar(String sqlFragment, Object value) {
        scalars.put(sqlFragment, value);
        return this;
    }

    RecordingJdbcTemplate updateCount(String sqlFragment, int count) {
        updateCounts.put(sqlFragment, count);
        return this;
    }

    RecordingJdbcTemplate fail(String sqlFragment, DataAccessException failure) {
        failures.put(sqlFragment, failure);
        return this;
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        record(sql, args);
        List<Map<String, Object>> answer = lookup(rows, sql);
        List<T> mapped = new ArrayList<>();
        for (int i = 0; answer != null && i < answer.size(); i++) {
            try {
                mapped.add(rowMapper.mapRow(resultSet(answer.get(i)), i));
            } catch (SQLException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return mapped;
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper) {
        return query(sql, rowMapper, new Object[0]);
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> found = query(sql, rowMapper, args);
        if (found.isEmpty()) {
            throw new EmptyResultDataAccessException(1);
        }
        return found.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
        record(sql, args);
        return (T) lookup(scalars, sql);
    }

    @Override
    public int update(String sql, Object... args) {
        record(sql, args);
        Integer count = lookup(updateCounts, sql);
        return count == null ? 1 : count;
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
        batchArgs.forEach(args -> record(sql, args));
        int[] counts = new int[batchArgs.size()];
        Arrays.fill(counts, 1);
        return counts;
    }

    Call last() {
        return calls.get(calls.size() - 1);
    }

    /** Statements whose SQL contains the fragment (whitespace collapsed), in order. */
    List<Call> callsWith(String sqlFragment) {
        return calls.stream().filter(call -> call.sql().contains(sqlFragment)).toList();
    }

    private void record(String sql, Object[] args) {
        String normalized = sql.replaceAll("\\s+", " ").trim();
        calls.add(new Call(normalized, args));
        DataAccessException failure = lookup(failures, normalized);
        if (failure != null) {
            throw failure;
        }
    }

    private static <V> V lookup(Map<String, V> answers, String sql) {
        String normalized = sql.replaceAll("\\s+", " ").trim();
        return answers.entrySet().stream()
                .filter(entry -> normalized.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /** A one-row ResultSet over the map; unknown columns throw like a real driver. */
    private static ResultSet resultSet(Map<String, Object> row) {
        Object[] lastValue = new Object[1];
        return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("wasNull")) {
                        return lastValue[0] == null;
                    }
                    if (args == null || !(args[0] instanceof String column)) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    if (!row.containsKey(column)) {
                        throw new SQLException("Unknown column " + column);
                    }
                    Object value = row.get(column);
                    lastValue[0] = value;
                    return switch (method.getName()) {
                        case "getInt" -> value == null ? 0 : ((Number) value).intValue();
                        case "getLong" -> value == null ? 0L : ((Number) value).longValue();
                        case "getBigDecimal" -> value == null ? null : new BigDecimal(value.toString());
                        case "getString", "getBytes" -> value;
                        case "getObject" -> args.length == 2 && value != null ? ((Class<?>) args[1]).cast(value) : value;
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
    }

    record Call(String sql, Object[] args) {
    }
}
