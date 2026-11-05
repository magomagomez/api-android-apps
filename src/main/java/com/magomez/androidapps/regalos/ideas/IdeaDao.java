package com.magomez.androidapps.regalos.ideas;

import com.magomez.androidapps.regalos.claims.ClaimState;
import com.magomez.androidapps.regalos.claims.Giver;
import com.magomez.androidapps.regalos.items.ValidItem;
import com.magomez.androidapps.regalos.people.Birthday;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.magomez.androidapps.regalos.config.RegalosColumns.instant;

/**
 * regalos_ideas, with whom each is shared and who gives it. Every read goes through {@link #SEEN_BY}:
 * an idea reaches its author and the people it is shared with, and never the person it is for, even
 * if a share row said so (R7, R13). Writes to the idea itself filter by its author.
 */
@Repository
public class IdeaDao {

    /** Three binds, all "me": not for me, and mine or shared with me. */
    private static final String SEEN_BY = """
             d.for_user_id <> ? AND (d.author_user_id = ?
               OR EXISTS (SELECT 1 FROM regalos_idea_shares s WHERE s.idea_id = d.id AND s.user_id = ?))
            """;
    private static final String SELECT = """
            SELECT d.id, d.for_user_id, d.title, d.url, d.store, d.price_cents, d.notes, d.bought, d.created_at,
                   d.photo_updated_at, d.author_user_id, au.display_name AS author_name,
                   c.state AS claim_state, c.user_id AS claim_user_id, cu.display_name AS claim_user_name
            FROM regalos_ideas d
            JOIN regalos_users au ON au.id = d.author_user_id
            LEFT JOIN regalos_idea_claims c ON c.idea_id = d.id
            LEFT JOIN regalos_users cu ON cu.id = c.user_id
            """;
    private static final String ORDER = " ORDER BY d.created_at, d.id";
    private static final String FOR_PERSON = SELECT + " WHERE d.for_user_id = ? AND" + SEEN_BY + ORDER;
    private static final String BY_ID = SELECT + " WHERE d.id = ? AND" + SEEN_BY;
    /** My gifts: the ideas I wrote, and the shared ones I give. */
    private static final String FOR_MY_GIFTS = """
            SELECT d.id, d.for_user_id, d.title, d.url, d.store, d.price_cents, d.notes, d.bought, d.created_at,
                   d.photo_updated_at, d.author_user_id, au.display_name AS author_name,
                   c.state AS claim_state, c.user_id AS claim_user_id, cu.display_name AS claim_user_name,
                   u.display_name AS for_name, u.birthday_day AS for_birthday_day, u.birthday_month AS for_birthday_month
            FROM regalos_ideas d
            JOIN regalos_users au ON au.id = d.author_user_id
            JOIN regalos_users u ON u.id = d.for_user_id
            LEFT JOIN regalos_idea_claims c ON c.idea_id = d.id
            LEFT JOIN regalos_users cu ON cu.id = c.user_id
            WHERE d.for_user_id <> ? AND (d.author_user_id = ? OR c.user_id = ?)
            ORDER BY d.created_at, d.id
            """;
    private static final String SHARES_FOR_PERSON = """
            SELECT s.idea_id, s.user_id FROM regalos_idea_shares s JOIN regalos_ideas d ON d.id = s.idea_id
            WHERE d.author_user_id = ? AND d.for_user_id = ? ORDER BY s.idea_id, s.user_id
            """;
    private static final String SHARES_OF = """
            SELECT s.idea_id, s.user_id FROM regalos_idea_shares s JOIN regalos_ideas d ON d.id = s.idea_id
            WHERE d.author_user_id = ? AND s.idea_id = ? ORDER BY s.user_id
            """;
    private static final String INSERT = """
            INSERT INTO regalos_ideas (author_user_id, for_user_id, title, url, store, price_cents, notes, bought)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
            """;
    /** R14: the author cannot mark it bought while someone it is shared with gives it. */
    private static final String UPDATE = """
            UPDATE regalos_ideas SET title = ?, url = ?, store = ?, price_cents = ?, notes = ?, bought = ?,
                updated_at = now()
            WHERE id = ? AND author_user_id = ?
              AND (? = FALSE OR NOT EXISTS (SELECT 1 FROM regalos_idea_claims c WHERE c.idea_id = regalos_ideas.id))
            """;
    private static final String DELETE = "DELETE FROM regalos_ideas WHERE id = ? AND author_user_id = ?";
    private static final String INSERT_SHARE = """
            INSERT INTO regalos_idea_shares (idea_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING
            """;
    private static final String DELETE_SHARE = "DELETE FROM regalos_idea_shares WHERE idea_id = ? AND user_id = ?";
    /** R14 in SQL: only someone it is shared with, only while its author has not bought it, only one. */
    private static final String INSERT_CLAIM = """
            INSERT INTO regalos_idea_claims (idea_id, user_id, state)
            SELECT ?, ?, ?
            WHERE EXISTS (SELECT 1 FROM regalos_idea_shares s WHERE s.idea_id = ? AND s.user_id = ?)
              AND NOT EXISTS (SELECT 1 FROM regalos_ideas d WHERE d.id = ? AND d.bought)
            ON CONFLICT (idea_id) DO NOTHING
            """;
    private static final String UPDATE_CLAIM = "UPDATE regalos_idea_claims SET state = ? WHERE idea_id = ? AND user_id = ?";
    private static final String DELETE_CLAIM = "DELETE FROM regalos_idea_claims WHERE idea_id = ? AND user_id = ?";

    private final JdbcTemplate jdbcTemplate;

    public IdeaDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** The ideas I see for that person: mine and those shared with me. */
    List<Idea> findFor(int meId, int forUserId) {
        return jdbcTemplate.query(FOR_PERSON, ideaFor(meId), forUserId, meId, meId, meId);
    }

    /** One idea I can see; empty to anyone else. */
    Optional<Idea> find(int meId, long id) {
        return jdbcTemplate.query(BY_ID, ideaFor(meId), id, meId, meId, meId).stream().findFirst();
    }

    /** My gifts: every idea I wrote and the shared ideas I give. */
    public List<IdeaWithPerson> findForMyGifts(int meId) {
        RowMapper<Idea> idea = ideaFor(meId);
        return jdbcTemplate.query(FOR_MY_GIFTS, (rs, rowNum) -> new IdeaWithPerson(idea.mapRow(rs, rowNum),
                rs.getString("for_name"), Birthday.ofColumns(rs.getObject("for_birthday_day", Integer.class),
                rs.getObject("for_birthday_month", Integer.class))), meId, meId, meId);
    }

    /** With whom each of my ideas for that person is shared, by idea id. */
    Map<Long, List<Integer>> sharesFor(int authorId, int forUserId) {
        return shares(jdbcTemplate.query(SHARES_FOR_PERSON, SHARE, authorId, forUserId));
    }

    /** With whom one of my ideas is shared. */
    List<Integer> sharesOf(int authorId, long ideaId) {
        return jdbcTemplate.query(SHARES_OF, SHARE, authorId, ideaId).stream().map(Share::userId).toList();
    }

    long insert(int authorId, int forUserId, ValidItem idea, boolean bought) {
        Long id = jdbcTemplate.queryForObject(INSERT, Long.class, authorId, forUserId, idea.title(), idea.url(),
                idea.store(), idea.priceCents(), idea.notes(), bought);
        return id == null ? 0 : id;
    }

    /** False when it is not mine, is gone, or would be bought while someone else gives it. */
    boolean update(int authorId, long id, ValidItem idea, boolean bought) {
        return jdbcTemplate.update(UPDATE, idea.title(), idea.url(), idea.store(), idea.priceCents(), idea.notes(),
                bought, id, authorId, bought) > 0;
    }

    boolean delete(int authorId, long id) {
        return jdbcTemplate.update(DELETE, id, authorId) > 0;
    }

    /** The idea is shared with exactly these people; whoever leaves takes their claim with them. */
    void setShares(long ideaId, List<Integer> current, List<Integer> wanted) {
        for (Integer gone : current) {
            if (!wanted.contains(gone)) {
                jdbcTemplate.update(DELETE_CLAIM, ideaId, gone);
                jdbcTemplate.update(DELETE_SHARE, ideaId, gone);
            }
        }
        for (Integer added : wanted) {
            if (!current.contains(added)) {
                jdbcTemplate.update(INSERT_SHARE, ideaId, added);
            }
        }
    }

    /** False when another claim got there first, it is not shared with me, or its author bought it. */
    boolean insertClaim(long ideaId, int userId, ClaimState state) {
        return jdbcTemplate.update(INSERT_CLAIM, ideaId, userId, state.name(), ideaId, userId, ideaId) > 0;
    }

    boolean updateClaim(long ideaId, int userId, ClaimState state) {
        return jdbcTemplate.update(UPDATE_CLAIM, state.name(), ideaId, userId) > 0;
    }

    boolean deleteClaim(long ideaId, int userId) {
        return jdbcTemplate.update(DELETE_CLAIM, ideaId, userId) > 0;
    }

    /** An idea as meId sees it; sharedWith is filled in later, and only for its author. */
    private static RowMapper<Idea> ideaFor(int meId) {
        return (rs, rowNum) -> {
            int authorId = rs.getInt("author_user_id");
            return new Idea(rs.getLong("id"), rs.getInt("for_user_id"), rs.getString("title"), rs.getString("url"),
                    rs.getString("store"), rs.getObject("price_cents", Integer.class), rs.getString("notes"),
                    rs.getBoolean("bought"), rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                    instant(rs.getObject("photo_updated_at", OffsetDateTime.class)),
                    new IdeaAuthor(authorId, rs.getString("author_name")), authorId == meId, null, claimOf(rs, meId));
        };
    }

    private static IdeaClaim claimOf(ResultSet rs, int meId) throws SQLException {
        String state = rs.getString("claim_state");
        if (state == null) {
            return null;
        }
        int giver = rs.getInt("claim_user_id");
        return new IdeaClaim(ClaimState.valueOf(state), new Giver(giver, rs.getString("claim_user_name")), giver == meId);
    }

    private static final RowMapper<Share> SHARE = (rs, rowNum) -> new Share(rs.getLong("idea_id"), rs.getInt("user_id"));

    private static Map<Long, List<Integer>> shares(List<Share> rows) {
        Map<Long, List<Integer>> byIdea = new LinkedHashMap<>();
        rows.forEach(row -> byIdea.computeIfAbsent(row.ideaId(), id -> new ArrayList<>()).add(row.userId()));
        return byIdea;
    }

    private record Share(long ideaId, int userId) {
    }
}
