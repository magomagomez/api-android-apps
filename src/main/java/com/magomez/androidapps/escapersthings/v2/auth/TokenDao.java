package com.magomez.androidapps.escapersthings.v2.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Sessions of escapers-api v2, stored by the SHA-256 of their token (never the token). */
@Repository
public class TokenDao {

    private static final String INSERT = "INSERT INTO escapers_tokens (token_hash, user_id) VALUES (?, ?)";
    /** Finds the session and records its use in one round trip. */
    private static final String TOUCH = """
            WITH used AS (
                UPDATE escapers_tokens SET last_used_at = now() WHERE token_hash = ? RETURNING user_id, token_hash
            )
            SELECT u.id, u.name, used.token_hash
            FROM used
            JOIN users u ON u.id = used.user_id
            """;
    private static final String DELETE = "DELETE FROM escapers_tokens WHERE token_hash = ?";
    private static final String DELETE_OTHERS = "DELETE FROM escapers_tokens WHERE user_id = ? AND token_hash <> ?";

    private static final RowMapper<CurrentUser> CURRENT_USER = (rs, rowNum) ->
            new CurrentUser(rs.getInt("id"), rs.getString("name"), rs.getString("token_hash"));

    private final JdbcTemplate jdbcTemplate;

    public TokenDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String tokenHash, int userId) {
        jdbcTemplate.update(INSERT, tokenHash, userId);
    }

    /** Empty when no session has that hash (unknown or revoked token). */
    public Optional<CurrentUser> touch(String tokenHash) {
        return jdbcTemplate.query(TOUCH, CURRENT_USER, tokenHash).stream().findFirst();
    }

    public void delete(String tokenHash) {
        jdbcTemplate.update(DELETE, tokenHash);
    }

    public void deleteOthers(int userId, String keepTokenHash) {
        jdbcTemplate.update(DELETE_OTHERS, userId, keepTokenHash);
    }
}
