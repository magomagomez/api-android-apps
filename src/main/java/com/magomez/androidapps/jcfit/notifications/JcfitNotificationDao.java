package com.magomez.androidapps.jcfit.notifications;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/** jcfit_notifications: the notices sent from the backoffice. */
@Repository
public class JcfitNotificationDao {

    private static final String COLUMNS = "id, title, body, sent_at, delivered, failed";
    /** The id travels in the push before the row exists: a failed send only leaves a gap. */
    private static final String NEXT_ID = "SELECT nextval(pg_get_serial_sequence('jcfit_notifications', 'id'))";
    private static final String INSERT = "INSERT INTO jcfit_notifications (id, title, body, delivered, failed) "
            + "VALUES (?, ?, ?, ?, ?) RETURNING " + COLUMNS;
    private static final String RECENT =
            "SELECT " + COLUMNS + " FROM jcfit_notifications ORDER BY sent_at DESC, id DESC LIMIT ?";

    private static final RowMapper<AdminNotification> NOTIFICATION = (rs, rowNum) -> new AdminNotification(
            rs.getInt("id"), rs.getString("title"), rs.getString("body"),
            rs.getObject("sent_at", OffsetDateTime.class).toInstant(), rs.getInt("delivered"), rs.getInt("failed"));

    private final JdbcTemplate jdbcTemplate;

    public JcfitNotificationDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    long nextId() {
        Long id = jdbcTemplate.queryForObject(NEXT_ID, Long.class);
        if (id == null) {
            throw new IllegalStateException("nextval returned no value");
        }
        return id;
    }

    AdminNotification insert(long id, String title, String body, int delivered, int failed) {
        return jdbcTemplate.queryForObject(INSERT, NOTIFICATION, id, title, body, delivered, failed);
    }

    List<AdminNotification> findRecent(int limit) {
        return jdbcTemplate.query(RECENT, NOTIFICATION, limit);
    }
}
