package com.magomez.androidapps.jcfit.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JcfitUserDao {

    private static final String BY_USERNAME =
            "SELECT id, username, password_hash FROM jcfit_users WHERE username = ?";
    private static final String ANY_USER = "SELECT EXISTS (SELECT 1 FROM jcfit_users)";
    /** Only inserts into an empty table, so two instances starting at once create one user. */
    private static final String INSERT_FIRST = """
            INSERT INTO jcfit_users (username, password_hash)
            SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM jcfit_users)
            """;

    private static final RowMapper<JcfitCredentials> CREDENTIALS = (rs, rowNum) ->
            new JcfitCredentials(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"));

    private final JdbcTemplate jdbcTemplate;

    public JcfitUserDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<JcfitCredentials> findByUsername(String username) {
        return jdbcTemplate.query(BY_USERNAME, CREDENTIALS, username).stream().findFirst();
    }

    boolean anyUser() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(ANY_USER, Boolean.class));
    }

    /** False when someone else created a user first. */
    boolean insertFirst(String username, String passwordHash) {
        return jdbcTemplate.update(INSERT_FIRST, username, passwordHash) > 0;
    }
}
