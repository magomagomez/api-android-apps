package com.magomez.androidapps.regalos.items;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The owner's side of regalos_items. Nothing here reads regalos_claims: what the owner gets
 * cannot carry a claim (R1). The only write to it is the EDITED notice, which returns nothing.
 */
@Repository
public class ItemDao {

    private static final String SELECT = """
            SELECT id, title, url, store, price_cents, image_url, notes, wanted_a_lot, status, created_at, updated_at
            """;
    private static final String MINE_BY_STATUS = SELECT
            + " FROM regalos_items WHERE owner_user_id = ? AND status = ? ORDER BY created_at, id";
    private static final String MINE_BY_ID = SELECT
            + " FROM regalos_items WHERE owner_user_id = ? AND id = ? AND status <> 'DELETED'";
    private static final String INSERT = """
            INSERT INTO regalos_items (owner_user_id, title, url, store, price_cents, image_url, notes, wanted_a_lot)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
            """;
    private static final String UPDATE = """
            UPDATE regalos_items SET title = ?, url = ?, store = ?, price_cents = ?, image_url = ?, notes = ?,
                wanted_a_lot = ?, updated_at = now()
            WHERE id = ? AND owner_user_id = ? AND status = 'ACTIVE'
            """;
    /** R6: whoever gives it sees "edited after you claimed it"; a no-op without a claim. */
    private static final String MARK_CLAIM_EDITED =
            "UPDATE regalos_claims SET item_edited_at = now() WHERE item_id = ?";
    private static final String SET_STATUS = """
            UPDATE regalos_items SET status = ?, updated_at = now()
            WHERE id = ? AND owner_user_id = ? AND status = ?
            """;
    /** R5: a claimed wish stays as DELETED so its giver hears of it... */
    private static final String SOFT_DELETE = """
            UPDATE regalos_items SET status = 'DELETED', updated_at = now()
            WHERE id = ? AND owner_user_id = ? AND status <> 'DELETED'
              AND EXISTS (SELECT 1 FROM regalos_claims c WHERE c.item_id = regalos_items.id)
            """;
    /** ...and an unclaimed one simply goes. */
    private static final String HARD_DELETE = """
            DELETE FROM regalos_items
            WHERE id = ? AND owner_user_id = ? AND status <> 'DELETED'
              AND NOT EXISTS (SELECT 1 FROM regalos_claims c WHERE c.item_id = regalos_items.id)
            """;

    private static final RowMapper<OwnItem> OWN_ITEM = (rs, rowNum) -> new OwnItem(rs.getLong("id"),
            rs.getString("title"), rs.getString("url"), rs.getString("store"),
            rs.getObject("price_cents", Integer.class), rs.getString("image_url"), rs.getString("notes"),
            rs.getBoolean("wanted_a_lot"), ItemStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", OffsetDateTime.class).toInstant(),
            rs.getObject("updated_at", OffsetDateTime.class).toInstant());

    private final JdbcTemplate jdbcTemplate;

    public ItemDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<OwnItem> findMine(int ownerId, ItemStatus status) {
        return jdbcTemplate.query(MINE_BY_STATUS, OWN_ITEM, ownerId, status.name());
    }

    Optional<OwnItem> findMine(int ownerId, long id) {
        return jdbcTemplate.query(MINE_BY_ID, OWN_ITEM, ownerId, id).stream().findFirst();
    }

    long insert(int ownerId, ValidItem item) {
        Long id = jdbcTemplate.queryForObject(INSERT, Long.class, ownerId, item.title(), item.url(), item.store(),
                item.priceCents(), item.imageUrl(), item.notes(), item.wantedALot());
        return id == null ? 0 : id;
    }

    /** False when the wish is not mine, does not exist or is not ACTIVE. */
    boolean update(int ownerId, long id, ValidItem item) {
        return jdbcTemplate.update(UPDATE, item.title(), item.url(), item.store(), item.priceCents(),
                item.imageUrl(), item.notes(), item.wantedALot(), id, ownerId) > 0;
    }

    void markClaimEdited(long id) {
        jdbcTemplate.update(MARK_CLAIM_EDITED, id);
    }

    boolean setStatus(int ownerId, long id, ItemStatus from, ItemStatus to) {
        return jdbcTemplate.update(SET_STATUS, to.name(), id, ownerId, from.name()) > 0;
    }

    boolean softDelete(int ownerId, long id) {
        return jdbcTemplate.update(SOFT_DELETE, id, ownerId) > 0;
    }

    boolean hardDelete(int ownerId, long id) {
        return jdbcTemplate.update(HARD_DELETE, id, ownerId) > 0;
    }
}
