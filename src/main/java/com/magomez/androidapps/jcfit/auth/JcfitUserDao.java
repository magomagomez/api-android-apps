package com.magomez.androidapps.jcfit.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The accounts are those of the shared users table (escapers-api v2 keeps their
 * password_hash). Which of them may use JCFit is {@link JcfitAuthService}'s call.
 */
@Repository
public class JcfitUserDao {

    private static final String BY_NAME = "SELECT id, name, password_hash FROM users WHERE name = ?";

    private static final RowMapper<JcfitCredentials> CREDENTIALS = (rs, rowNum) ->
            new JcfitCredentials(rs.getInt("id"), rs.getString("name"), rs.getString("password_hash"));

    private final JdbcTemplate jdbcTemplate;

    public JcfitUserDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<JcfitCredentials> findByName(String name) {
        return jdbcTemplate.query(BY_NAME, CREDENTIALS, name).stream().findFirst();
    }
}
