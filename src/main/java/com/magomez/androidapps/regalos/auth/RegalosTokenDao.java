package com.magomez.androidapps.regalos.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Sessions of regalos-api, stored by the SHA-256 of their token (never the token). */
@Repository
public class RegalosTokenDao {

    private static final String INSERT = "INSERT INTO regalos_tokens (token_hash, user_id) VALUES (?, ?)";
    /** Finds the session and records its use in one round trip. */
    private static final String TOUCH = """
            WITH used AS (
                UPDATE regalos_tokens SET last_used_at = now() WHERE token_hash = ? RETURNING user_id, token_hash
            )
            SELECT u.id, u.display_name, used.token_hash
            FROM used
            JOIN regalos_users u ON u.id = used.user_id
            """;
    private static final String DELETE = "DELETE FROM regalos_tokens WHERE token_hash = ?";

    private static final RowMapper<RegalosCurrentUser> CURRENT_USER = (rs, rowNum) ->
            new RegalosCurrentUser(rs.getInt("id"), rs.getString("display_name"), rs.getString("token_hash"));

    private final JdbcTemplate jdbcTemplate;

    public RegalosTokenDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insert(String tokenHash, int userId) {
        jdbcTemplate.update(INSERT, tokenHash, userId);
    }

    /** Empty when no session has that hash (unknown or revoked token). */
    Optional<RegalosCurrentUser> touch(String tokenHash) {
        return jdbcTemplate.query(TOUCH, CURRENT_USER, tokenHash).stream().findFirst();
    }

    void delete(String tokenHash) {
        jdbcTemplate.update(DELETE, tokenHash);
    }
}
