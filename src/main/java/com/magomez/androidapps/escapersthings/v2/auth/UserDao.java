package com.magomez.androidapps.escapersthings.v2.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserDao {

    private static final String SELECT_CREDENTIALS = "SELECT id, name, password_hash FROM users";
    private static final String BY_NAME = " WHERE name = ?";
    private static final String BY_ID = " WHERE id = ?";
    private static final String SELECT_USERS = "SELECT id, name FROM users ORDER BY id";
    private static final String UPDATE_PASSWORD_HASH = "UPDATE users SET password_hash = ? WHERE id = ?";
    private static final String SELECT_WITHOUT_HASH = "SELECT id, password FROM users WHERE password_hash IS NULL ORDER BY id";
    /** Only fills an empty hash, so a backfill never overwrites a password changed meanwhile. */
    private static final String FILL_PASSWORD_HASH =
            "UPDATE users SET password_hash = ? WHERE id = ? AND password_hash IS NULL";

    private static final RowMapper<Credentials> CREDENTIALS = (rs, rowNum) ->
            new Credentials(rs.getInt("id"), rs.getString("name"), rs.getString("password_hash"));
    private static final RowMapper<UserDTO> USER = (rs, rowNum) -> new UserDTO(rs.getInt("id"), rs.getString("name"));
    private static final RowMapper<PlainPassword> PLAIN_PASSWORD = (rs, rowNum) ->
            new PlainPassword(rs.getInt("id"), rs.getString("password"));

    private final JdbcTemplate jdbcTemplate;

    public UserDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Empty unless exactly one user has that name. */
    public Optional<Credentials> findByName(String name) {
        List<Credentials> found = jdbcTemplate.query(SELECT_CREDENTIALS + BY_NAME, CREDENTIALS, name);
        return found.size() == 1 ? Optional.of(found.get(0)) : Optional.empty();
    }

    public Optional<Credentials> findById(int id) {
        return jdbcTemplate.query(SELECT_CREDENTIALS + BY_ID, CREDENTIALS, id).stream().findFirst();
    }

    public List<UserDTO> findAll() {
        return jdbcTemplate.query(SELECT_USERS, USER);
    }

    public void updatePasswordHash(int id, String passwordHash) {
        jdbcTemplate.update(UPDATE_PASSWORD_HASH, passwordHash, id);
    }

    public List<PlainPassword> findWithoutPasswordHash() {
        return jdbcTemplate.query(SELECT_WITHOUT_HASH, PLAIN_PASSWORD);
    }

    /** False when the user already had a hash. */
    public boolean fillPasswordHash(int id, String passwordHash) {
        return jdbcTemplate.update(FILL_PASSWORD_HASH, passwordHash, id) > 0;
    }

    /** The legacy plain-text password of a user still without hash. */
    public record PlainPassword(int id, String password) {
    }
}
