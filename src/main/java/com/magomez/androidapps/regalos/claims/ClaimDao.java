package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.items.ItemStatus;
import com.magomez.androidapps.regalos.people.Birthday;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** The givers' side: wishes of others with their claims, and regalos_claims itself. */
@Repository
public class ClaimDao {

    private static final String SELECT = """
            SELECT i.id, i.owner_user_id, i.title, i.url, i.store, i.price_cents, i.image_url, i.notes,
                   i.wanted_a_lot, i.status,
                   o.display_name AS owner_name, o.birthday_day AS owner_birthday_day,
                   o.birthday_month AS owner_birthday_month,
                   c.state AS claim_state, c.marked_by_user_id, c.giver_user_id,
                   COALESCE(g.display_name, c.giver_name) AS giver_name, c.item_edited_at
            FROM regalos_items i
            JOIN regalos_users o ON o.id = i.owner_user_id
            LEFT JOIN regalos_claims c ON c.item_id = i.id
            LEFT JOIN regalos_users g ON g.id = c.giver_user_id
            """;
    private static final String ORDER = " ORDER BY i.created_at, i.id";
    private static final String ITEMS_OF = SELECT + " WHERE i.owner_user_id = ? AND i.status = 'ACTIVE'" + ORDER;
    private static final String ITEM_BY_ID = SELECT + " WHERE i.id = ?";
    /** The gifts I give, plus those I noted for someone outside the app. */
    private static final String MY_CLAIMS = SELECT
            + " WHERE c.giver_user_id = ? OR (c.giver_user_id IS NULL AND c.marked_by_user_id = ?)" + ORDER;
    /** R2 under a race: the primary key on item_id lets only one claim in; the loser inserts 0 rows. */
    private static final String INSERT = """
            INSERT INTO regalos_claims (item_id, marked_by_user_id, giver_user_id, giver_name, state)
            VALUES (?, ?, ?, ?, ?) ON CONFLICT (item_id) DO NOTHING
            """;
    /** R4 again in SQL, so a claim taken over meanwhile is not overwritten. */
    private static final String UPDATE = """
            UPDATE regalos_claims SET giver_user_id = ?, giver_name = ?, state = ?, item_edited_at = NULL,
                updated_at = now()
            WHERE item_id = ? AND (marked_by_user_id = ? OR giver_user_id = ?)
            """;
    private static final String DELETE = """
            DELETE FROM regalos_claims WHERE item_id = ? AND (marked_by_user_id = ? OR giver_user_id = ?)
            """;
    private static final String PURGE_DELETED_ITEM = """
            DELETE FROM regalos_items
            WHERE id = ? AND status = 'DELETED'
              AND NOT EXISTS (SELECT 1 FROM regalos_claims c WHERE c.item_id = regalos_items.id)
            """;

    private static final RowMapper<ClaimRow> ROW = (rs, rowNum) -> {
        String state = rs.getString("claim_state");
        return new ClaimRow(rs.getLong("id"), rs.getInt("owner_user_id"), rs.getString("owner_name"),
                Birthday.ofColumns(rs.getObject("owner_birthday_day", Integer.class),
                        rs.getObject("owner_birthday_month", Integer.class)),
                rs.getString("title"), rs.getString("url"), rs.getString("store"),
                rs.getObject("price_cents", Integer.class), rs.getString("image_url"), rs.getString("notes"),
                rs.getBoolean("wanted_a_lot"), ItemStatus.valueOf(rs.getString("status")),
                state == null ? null : ClaimState.valueOf(state),
                rs.getObject("marked_by_user_id", Integer.class), rs.getObject("giver_user_id", Integer.class),
                rs.getString("giver_name"), rs.getObject("item_edited_at") != null);
    };

    private final JdbcTemplate jdbcTemplate;

    public ClaimDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<ClaimRow> activeItemsOf(int ownerId) {
        return jdbcTemplate.query(ITEMS_OF, ROW, ownerId);
    }

    /** Whatever its status: callers decide what they may do with it. */
    Optional<ClaimRow> item(long id) {
        return jdbcTemplate.query(ITEM_BY_ID, ROW, id).stream().findFirst();
    }

    List<ClaimRow> myClaims(int meId) {
        return jdbcTemplate.query(MY_CLAIMS, ROW, meId, meId);
    }

    /** False when another claim got there first. */
    boolean insert(long itemId, int markedBy, Integer giverUserId, String giverName, ClaimState state) {
        return jdbcTemplate.update(INSERT, itemId, markedBy, giverUserId, giverName, state.name()) > 0;
    }

    boolean update(long itemId, int editor, Integer giverUserId, String giverName, ClaimState state) {
        return jdbcTemplate.update(UPDATE, giverUserId, giverName, state.name(), itemId, editor, editor) > 0;
    }

    boolean delete(long itemId, int editor) {
        return jdbcTemplate.update(DELETE, itemId, editor, editor) > 0;
    }

    void purgeDeletedItem(long itemId) {
        jdbcTemplate.update(PURGE_DELETED_ITEM, itemId);
    }
}
