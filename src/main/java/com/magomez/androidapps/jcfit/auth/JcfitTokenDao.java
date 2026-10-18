package com.magomez.androidapps.jcfit.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Sessions of jcfit-api, stored by the SHA-256 of their token (never the token). */
@Repository
public class JcfitTokenDao {

    private static final String INSERT = "INSERT INTO jcfit_tokens (token_hash, user_id) VALUES (?, ?)";
    /** Finds the session and records its use in one round trip. */
    private static final String TOUCH = """
            WITH used AS (
                UPDATE jcfit_tokens SET last_used_at = now() WHERE token_hash = ? RETURNING user_id, token_hash
            )
            SELECT u.id, u.name, used.token_hash
            FROM used
            JOIN users u ON u.id = used.user_id
            """;
    private static final String DELETE = "DELETE FROM jcfit_tokens WHERE token_hash = ?";

    private static final RowMapper<JcfitCurrentUser> CURRENT_USER = (rs, rowNum) ->
            new JcfitCurrentUser(rs.getInt("id"), rs.getString("name"), rs.getString("token_hash"));

    private final JdbcTemplate jdbcTemplate;

    public JcfitTokenDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insert(String tokenHash, int userId) {
        jdbcTemplate.update(INSERT, tokenHash, userId);
    }

    /** Empty when no session has that hash (unknown or revoked token). */
    Optional<JcfitCurrentUser> touch(String tokenHash) {
        return jdbcTemplate.query(TOUCH, CURRENT_USER, tokenHash).stream().findFirst();
    }

    void delete(String tokenHash) {
        jdbcTemplate.update(DELETE, tokenHash);
    }
}
