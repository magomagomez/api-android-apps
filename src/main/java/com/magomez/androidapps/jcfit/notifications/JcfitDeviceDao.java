package com.magomez.androidapps.jcfit.notifications;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** jcfit_devices: one FCM token per installation of the app, with the account that registered it. */
@Repository
public class JcfitDeviceDao {

    /** A token that comes back (or moves to another account) keeps its row and is refreshed. */
    private static final String UPSERT = """
            INSERT INTO jcfit_devices (token, user_id) VALUES (?, ?)
            ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, updated_at = now()
            """;
    private static final String DELETE = "DELETE FROM jcfit_devices WHERE token = ?";
    private static final String TOKENS_OF_USER =
            "SELECT token FROM jcfit_devices WHERE user_id = ? ORDER BY created_at, token";

    private final JdbcTemplate jdbcTemplate;

    public JcfitDeviceDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void upsert(String token, int userId) {
        jdbcTemplate.update(UPSERT, token, userId);
    }

    void delete(String token) {
        jdbcTemplate.update(DELETE, token);
    }

    List<String> tokensOf(int userId) {
        return jdbcTemplate.query(TOKENS_OF_USER, (rs, rowNum) -> rs.getString("token"), userId);
    }

    void deleteAll(List<String> tokens) {
        if (!tokens.isEmpty()) {
            jdbcTemplate.batchUpdate(DELETE, tokens.stream().map(token -> new Object[]{token}).toList());
        }
    }
}
