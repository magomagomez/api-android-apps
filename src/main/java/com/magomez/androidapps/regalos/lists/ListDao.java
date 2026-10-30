package com.magomez.androidapps.regalos.lists;

import com.magomez.androidapps.regalos.items.ValidItem;
import com.magomez.androidapps.regalos.people.Birthday;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** regalos_lists, their members and their wishes (regalos_items with list_id). */
@Repository
public class ListDao {

    /** Only the lists the caller is a member of (R8), with the counts of their ACTIVE wishes. */
    private static final String SUMMARIES = """
            SELECT l.id, l.name, l.birthday_day, l.birthday_month, l.created_by_user_id,
                   cu.display_name AS created_by_name,
                   (SELECT count(*) FROM regalos_list_members m2 WHERE m2.list_id = l.id) AS member_count,
                   count(i.id) AS wish_count,
                   count(i.id) FILTER (WHERE i.id IS NOT NULL AND c.item_id IS NULL) AS free_count,
                   count(i.id) FILTER (WHERE c.giver_user_id = ?) AS mine_count
            FROM regalos_lists l JOIN regalos_list_members m ON m.list_id = l.id AND m.user_id = ?
            JOIN regalos_users cu ON cu.id = l.created_by_user_id
            LEFT JOIN regalos_items i ON i.list_id = l.id AND i.status = 'ACTIVE'
            LEFT JOIN regalos_claims c ON c.item_id = i.id
            """;
    private static final String GROUP = " GROUP BY l.id, l.name, l.birthday_day, l.birthday_month, l.created_by_user_id, cu.display_name";
    private static final String ONE = " WHERE l.id = ?";
    private static final String IS_MEMBER = "SELECT count(*) FROM regalos_list_members WHERE list_id = ? AND user_id = ?";
    private static final String MEMBERS = """
            SELECT u.id, u.username, u.display_name, u.birthday_day, u.birthday_month
            FROM regalos_list_members m JOIN regalos_users u ON u.id = m.user_id
            WHERE m.list_id = ? ORDER BY u.display_name
            """;
    private static final String INSERT_LIST = """
            INSERT INTO regalos_lists (name, birthday_day, birthday_month, created_by_user_id)
            VALUES (?, ?, ?, ?) RETURNING id
            """;
    private static final String UPDATE_LIST = "UPDATE regalos_lists SET name = ?, birthday_day = ?, birthday_month = ? WHERE id = ?";
    private static final String DELETE_LIST = "DELETE FROM regalos_lists WHERE id = ?";
    private static final String INSERT_MEMBER = "INSERT INTO regalos_list_members (list_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
    private static final String DELETE_MEMBER = "DELETE FROM regalos_list_members WHERE list_id = ? AND user_id = ?";
    /** Someone leaving takes their gifts with them; claims they made for another member stay. */
    private static final String DELETE_CLAIMS = """
            DELETE FROM regalos_claims c USING regalos_items i
            WHERE c.item_id = i.id AND i.list_id = ?
              AND (c.giver_user_id = ? OR (c.giver_user_id IS NULL AND c.marked_by_user_id = ?))
            """;
    private static final String INSERT_ITEM = """
            INSERT INTO regalos_items (list_id, added_by_user_id, title, url, store, price_cents, image_url, notes, wanted_a_lot)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
            """;
    private static final String UPDATE_ITEM = """
            UPDATE regalos_items SET title = ?, url = ?, store = ?, price_cents = ?, image_url = ?, notes = ?,
                wanted_a_lot = ?, updated_at = now()
            WHERE id = ? AND list_id = ? AND status = 'ACTIVE'
            """;
    private static final String MARK_CLAIM_EDITED = "UPDATE regalos_claims SET item_edited_at = now() WHERE item_id = ?";
    private static final String SOFT_DELETE_ITEM = """
            UPDATE regalos_items SET status = 'DELETED', updated_at = now()
            WHERE id = ? AND list_id = ? AND status <> 'DELETED'
              AND EXISTS (SELECT 1 FROM regalos_claims c WHERE c.item_id = regalos_items.id)
            """;
    private static final String HARD_DELETE_ITEM = """
            DELETE FROM regalos_items
            WHERE id = ? AND list_id = ? AND status <> 'DELETED'
              AND NOT EXISTS (SELECT 1 FROM regalos_claims c WHERE c.item_id = regalos_items.id)
            """;

    private static final RowMapper<ListRow> LIST = (rs, rowNum) -> new ListRow(rs.getLong("id"), rs.getString("name"),
            Birthday.ofColumns(rs.getObject("birthday_day", Integer.class), rs.getObject("birthday_month", Integer.class)),
            rs.getInt("created_by_user_id"), rs.getString("created_by_name"), rs.getInt("member_count"),
            rs.getInt("wish_count"), rs.getInt("free_count"), rs.getInt("mine_count"));
    private static final RowMapper<Member> MEMBER = (rs, rowNum) -> new Member(rs.getInt("id"), rs.getString("display_name"),
            Birthday.ofColumns(rs.getObject("birthday_day", Integer.class), rs.getObject("birthday_month", Integer.class)));

    private final JdbcTemplate jdbcTemplate;

    public ListDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<ListRow> findMine(int meId) {
        return jdbcTemplate.query(SUMMARIES + GROUP, LIST, meId, meId);
    }

    Optional<ListRow> find(int meId, long listId) {
        return jdbcTemplate.query(SUMMARIES + ONE + GROUP, LIST, meId, meId, listId).stream().findFirst();
    }

    public boolean isMember(long listId, int userId) {
        Integer count = jdbcTemplate.queryForObject(IS_MEMBER, Integer.class, listId, userId);
        return count != null && count > 0;
    }

    List<Member> members(long listId) {
        return jdbcTemplate.query(MEMBERS, MEMBER, listId);
    }

    long insert(String name, Birthday birthday, int creatorId) {
        Long id = jdbcTemplate.queryForObject(INSERT_LIST, Long.class, name, day(birthday), month(birthday), creatorId);
        return id == null ? 0 : id;
    }

    void update(long listId, String name, Birthday birthday) {
        jdbcTemplate.update(UPDATE_LIST, name, day(birthday), month(birthday), listId);
    }

    void delete(long listId) {
        jdbcTemplate.update(DELETE_LIST, listId);
    }

    void addMember(long listId, int userId) {
        jdbcTemplate.update(INSERT_MEMBER, listId, userId);
    }

    void removeMember(long listId, int userId) {
        jdbcTemplate.update(DELETE_CLAIMS, listId, userId, userId);
        jdbcTemplate.update(DELETE_MEMBER, listId, userId);
    }

    long insertItem(long listId, int addedBy, ValidItem item) {
        Long id = jdbcTemplate.queryForObject(INSERT_ITEM, Long.class, listId, addedBy, item.title(), item.url(),
                item.store(), item.priceCents(), item.imageUrl(), item.notes(), item.wantedALot());
        return id == null ? 0 : id;
    }

    boolean updateItem(long listId, long itemId, ValidItem item) {
        boolean updated = jdbcTemplate.update(UPDATE_ITEM, item.title(), item.url(), item.store(), item.priceCents(),
                item.imageUrl(), item.notes(), item.wantedALot(), itemId, listId) > 0;
        if (updated) {
            jdbcTemplate.update(MARK_CLAIM_EDITED, itemId);
        }
        return updated;
    }

    /** Like a wish of a letter (R5): kept as DELETED while someone has it claimed. */
    boolean deleteItem(long listId, long itemId) {
        return jdbcTemplate.update(SOFT_DELETE_ITEM, itemId, listId) > 0
                || jdbcTemplate.update(HARD_DELETE_ITEM, itemId, listId) > 0;
    }

    private static Integer day(Birthday birthday) {
        return birthday == null ? null : birthday.day();
    }

    private static Integer month(Birthday birthday) {
        return birthday == null ? null : birthday.month();
    }

    /** A member of a list. */
    record Member(int id, String displayName, Birthday birthday) {
    }
}
