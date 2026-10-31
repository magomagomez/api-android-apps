package com.magomez.androidapps.regalos.ideas;

import com.magomez.androidapps.regalos.items.ValidItem;
import com.magomez.androidapps.regalos.people.Birthday;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.magomez.androidapps.regalos.config.RegalosColumns.instant;

/**
 * regalos_ideas. Every statement filters by author_user_id: there is no way to read an idea
 * without being its author, so it cannot leak to the person it is for (R7).
 */
@Repository
public class IdeaDao {

    private static final String COLUMNS = """
            SELECT id, for_user_id, title, url, store, price_cents, notes, bought, created_at, photo_updated_at
            """;
    private static final String FOR_PERSON = COLUMNS
            + " FROM regalos_ideas WHERE author_user_id = ? AND for_user_id = ? ORDER BY created_at, id";
    private static final String BY_ID = COLUMNS + " FROM regalos_ideas WHERE author_user_id = ? AND id = ?";
    private static final String ALL_MINE = """
            SELECT d.id, d.for_user_id, d.title, d.url, d.store, d.price_cents, d.notes, d.bought, d.created_at,
                   d.photo_updated_at,
                   u.display_name AS for_name, u.birthday_day AS for_birthday_day, u.birthday_month AS for_birthday_month
            FROM regalos_ideas d
            JOIN regalos_users u ON u.id = d.for_user_id
            WHERE d.author_user_id = ?
            ORDER BY d.created_at, d.id
            """;
    private static final String INSERT = """
            INSERT INTO regalos_ideas (author_user_id, for_user_id, title, url, store, price_cents, notes, bought)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
            """;
    private static final String UPDATE = """
            UPDATE regalos_ideas SET title = ?, url = ?, store = ?, price_cents = ?, notes = ?, bought = ?,
                updated_at = now()
            WHERE id = ? AND author_user_id = ?
            """;
    private static final String DELETE = "DELETE FROM regalos_ideas WHERE id = ? AND author_user_id = ?";

    private static final RowMapper<Idea> IDEA = (rs, rowNum) -> new Idea(rs.getLong("id"), rs.getInt("for_user_id"),
            rs.getString("title"), rs.getString("url"), rs.getString("store"),
            rs.getObject("price_cents", Integer.class), rs.getString("notes"), rs.getBoolean("bought"),
            rs.getObject("created_at", OffsetDateTime.class).toInstant(),
            instant(rs.getObject("photo_updated_at", OffsetDateTime.class)));
    private static final RowMapper<IdeaWithPerson> WITH_PERSON = (rs, rowNum) -> new IdeaWithPerson(
            IDEA.mapRow(rs, rowNum), rs.getString("for_name"),
            Birthday.ofColumns(rs.getObject("for_birthday_day", Integer.class),
                    rs.getObject("for_birthday_month", Integer.class)));

    private final JdbcTemplate jdbcTemplate;

    public IdeaDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<Idea> findFor(int authorId, int forUserId) {
        return jdbcTemplate.query(FOR_PERSON, IDEA, authorId, forUserId);
    }

    Optional<Idea> find(int authorId, long id) {
        return jdbcTemplate.query(BY_ID, IDEA, authorId, id).stream().findFirst();
    }

    /** Every idea I wrote, for my gifts. */
    public List<IdeaWithPerson> findAllMine(int authorId) {
        return jdbcTemplate.query(ALL_MINE, WITH_PERSON, authorId);
    }

    long insert(int authorId, int forUserId, ValidItem idea, boolean bought) {
        Long id = jdbcTemplate.queryForObject(INSERT, Long.class, authorId, forUserId, idea.title(), idea.url(),
                idea.store(), idea.priceCents(), idea.notes(), bought);
        return id == null ? 0 : id;
    }

    boolean update(int authorId, long id, ValidItem idea, boolean bought) {
        return jdbcTemplate.update(UPDATE, idea.title(), idea.url(), idea.store(), idea.priceCents(), idea.notes(),
                bought, id, authorId) > 0;
    }

    boolean delete(int authorId, long id) {
        return jdbcTemplate.update(DELETE, id, authorId) > 0;
    }
}
