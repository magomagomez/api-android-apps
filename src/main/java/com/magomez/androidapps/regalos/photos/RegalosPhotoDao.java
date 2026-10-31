package com.magomez.androidapps.regalos.photos;

import com.magomez.androidapps.regalos.items.ItemStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The photo columns of regalos_items and regalos_ideas. Who may write is part of each statement:
 * the owner of a letter, the list a wish belongs to, the author of an idea (R7, R11).
 */
@Repository
public class RegalosPhotoDao {

    private static final String OWN_ITEM = """
            SELECT photo_file_id FROM regalos_items WHERE id = ? AND owner_user_id = ? AND status = 'ACTIVE'
            """;
    private static final String LIST_ITEM = """
            SELECT photo_file_id FROM regalos_items WHERE id = ? AND list_id = ? AND status = 'ACTIVE'
            """;
    private static final String IDEA = "SELECT photo_file_id FROM regalos_ideas WHERE id = ? AND author_user_id = ?";
    private static final String SET_OWN_ITEM = """
            UPDATE regalos_items SET photo_file_id = ?, photo_updated_at = ?
            WHERE id = ? AND owner_user_id = ? AND status = 'ACTIVE'
            """;
    private static final String SET_LIST_ITEM = """
            UPDATE regalos_items SET photo_file_id = ?, photo_updated_at = ?
            WHERE id = ? AND list_id = ? AND status = 'ACTIVE'
            """;
    private static final String SET_IDEA = """
            UPDATE regalos_ideas SET photo_file_id = ?, photo_updated_at = ? WHERE id = ? AND author_user_id = ?
            """;
    /** R6: a new photo is an edit too, for whoever gives the wish. */
    private static final String MARK_CLAIM_EDITED = "UPDATE regalos_claims SET item_edited_at = now() WHERE item_id = ?";
    /** A wish with what decides who may see its photo. */
    private static final String VIEWABLE_ITEM = """
            SELECT i.owner_user_id, i.list_id, i.status, i.photo_file_id, c.marked_by_user_id, c.giver_user_id
            FROM regalos_items i LEFT JOIN regalos_claims c ON c.item_id = i.id
            WHERE i.id = ?
            """;
    private static final String ITEM_FILE = "SELECT photo_file_id FROM regalos_items WHERE id = ?";
    private static final String LIST_FILES = """
            SELECT photo_file_id FROM regalos_items WHERE list_id = ? AND photo_file_id IS NOT NULL
            """;

    private static final RowMapper<Slot> SLOT = (rs, rowNum) -> new Slot(rs.getString("photo_file_id"));
    private static final RowMapper<ViewableItem> VIEWABLE = (rs, rowNum) -> new ViewableItem(
            rs.getObject("owner_user_id", Integer.class), rs.getObject("list_id", Long.class),
            ItemStatus.valueOf(rs.getString("status")), rs.getString("photo_file_id"),
            rs.getObject("marked_by_user_id", Integer.class), rs.getObject("giver_user_id", Integer.class));

    private final JdbcTemplate jdbcTemplate;

    public RegalosPhotoDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** An ACTIVE wish of my letter, with its current photo. */
    Optional<Slot> ownItem(int ownerId, long itemId) {
        return jdbcTemplate.query(OWN_ITEM, SLOT, itemId, ownerId).stream().findFirst();
    }

    /** An ACTIVE wish of that shared list; the caller has already checked I am invited. */
    Optional<Slot> listItem(long listId, long itemId) {
        return jdbcTemplate.query(LIST_ITEM, SLOT, itemId, listId).stream().findFirst();
    }

    /** One of my ideas. Another author's idea is simply not found (R7). */
    Optional<Slot> idea(int authorId, long ideaId) {
        return jdbcTemplate.query(IDEA, SLOT, ideaId, authorId).stream().findFirst();
    }

    /** A null fileId removes the photo. False when the wish is no longer there. */
    boolean setOwnItem(int ownerId, long itemId, String fileId, Instant at) {
        return jdbcTemplate.update(SET_OWN_ITEM, fileId, timestamp(fileId, at), itemId, ownerId) > 0;
    }

    boolean setListItem(long listId, long itemId, String fileId, Instant at) {
        return jdbcTemplate.update(SET_LIST_ITEM, fileId, timestamp(fileId, at), itemId, listId) > 0;
    }

    boolean setIdea(int authorId, long ideaId, String fileId, Instant at) {
        return jdbcTemplate.update(SET_IDEA, fileId, timestamp(fileId, at), ideaId, authorId) > 0;
    }

    void markClaimEdited(long itemId) {
        jdbcTemplate.update(MARK_CLAIM_EDITED, itemId);
    }

    Optional<ViewableItem> viewableItem(long itemId) {
        return jdbcTemplate.query(VIEWABLE_ITEM, VIEWABLE, itemId).stream().findFirst();
    }

    /** The photo of a wish, whoever it belongs to: only for cleaning up after a delete. */
    Optional<String> itemFile(long itemId) {
        return jdbcTemplate.query(ITEM_FILE, SLOT, itemId).stream().map(Slot::fileId).filter(Objects::nonNull)
                .findFirst();
    }

    List<String> listFiles(long listId) {
        return jdbcTemplate.query(LIST_FILES, SLOT, listId).stream().map(Slot::fileId).toList();
    }

    private static OffsetDateTime timestamp(String fileId, Instant at) {
        return fileId == null ? null : OffsetDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    /** The current photo of a wish or idea; fileId is null without one. */
    record Slot(String fileId) {
    }

    /** A wish as far as seeing its photo goes; owner or list is null, and so is a missing claim. */
    record ViewableItem(Integer ownerId, Long listId, ItemStatus status, String fileId, Integer markedBy,
                        Integer giverUserId) {
    }
}
