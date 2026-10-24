package com.magomez.androidapps.regalos.people;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Reads and writes regalos_users, the accounts of this app only (not the shared users table). */
@Repository
public class RegalosUserDao {

    private static final String CREDENTIALS = "SELECT id, password_hash FROM regalos_users WHERE username = ?";
    /** A taken username breaks UNIQUE (username): DuplicateKeyException. */
    private static final String INSERT = """
            INSERT INTO regalos_users (username, display_name, password_hash, birthday_day, birthday_month)
            VALUES (?, ?, ?, ?, ?) RETURNING id
            """;
    private static final String BY_ID = """
            SELECT id, username, display_name, birthday_day, birthday_month
            FROM regalos_users WHERE id = ?
            """;
    private static final String UPDATE_PROFILE = """
            UPDATE regalos_users SET display_name = ?, birthday_day = ?, birthday_month = ? WHERE id = ?
            """;
    /** Everybody but the caller, with the counts of their ACTIVE wishes. */
    private static final String OTHERS = """
            SELECT u.id, u.display_name, u.birthday_day, u.birthday_month,
                   count(i.id) AS wish_count,
                   count(i.id) FILTER (WHERE i.id IS NOT NULL AND c.item_id IS NULL) AS free_count,
                   count(i.id) FILTER (WHERE c.giver_user_id = ?) AS mine_count
            FROM regalos_users u
            LEFT JOIN regalos_items i ON i.owner_user_id = u.id AND i.status = 'ACTIVE'
            LEFT JOIN regalos_claims c ON c.item_id = i.id
            WHERE u.id <> ?
            GROUP BY u.id, u.display_name, u.birthday_day, u.birthday_month
            """;

    private static final RowMapper<UserCredentials> CREDENTIALS_ROW = (rs, rowNum) ->
            new UserCredentials(rs.getInt("id"), rs.getString("password_hash"));
    private static final RowMapper<Me> ME = (rs, rowNum) -> new Me(rs.getInt("id"), rs.getString("username"),
            rs.getString("display_name"), birthday(rs.getObject("birthday_day", Integer.class),
            rs.getObject("birthday_month", Integer.class)));
    private static final RowMapper<PersonRow> OTHER = (rs, rowNum) -> new PersonRow(rs.getInt("id"),
            rs.getString("display_name"), birthday(rs.getObject("birthday_day", Integer.class),
            rs.getObject("birthday_month", Integer.class)),
            rs.getInt("wish_count"), rs.getInt("free_count"), rs.getInt("mine_count"));

    private final JdbcTemplate jdbcTemplate;

    public RegalosUserDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** The username must already be in lower case. */
    public Optional<UserCredentials> findCredentials(String username) {
        return jdbcTemplate.query(CREDENTIALS, CREDENTIALS_ROW, username).stream().findFirst();
    }

    /** Returns the id of the new user. */
    public int insert(String username, String displayName, String passwordHash, Birthday birthday) {
        Integer id = jdbcTemplate.queryForObject(INSERT, Integer.class, username, displayName, passwordHash,
                day(birthday), month(birthday));
        return id == null ? 0 : id;
    }

    public Optional<Me> findMe(int id) {
        return jdbcTemplate.query(BY_ID, ME, id).stream().findFirst();
    }

    boolean updateProfile(int id, String displayName, Birthday birthday) {
        return jdbcTemplate.update(UPDATE_PROFILE, displayName, day(birthday), month(birthday), id) > 0;
    }

    List<PersonRow> findOthers(int meId) {
        return jdbcTemplate.query(OTHERS, OTHER, meId, meId);
    }

    private static Birthday birthday(Integer day, Integer month) {
        return Birthday.ofColumns(day, month);
    }

    private static Integer day(Birthday birthday) {
        return birthday == null ? null : birthday.day();
    }

    private static Integer month(Birthday birthday) {
        return birthday == null ? null : birthday.month();
    }
}
