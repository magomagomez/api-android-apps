package com.magomez.androidapps.regalos.items;

import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Map;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static com.magomez.androidapps.regalos.items.ItemRulesTest.request;
import static org.assertj.core.api.Assertions.assertThat;

/** My letter: my own wishes, which never carry anything about who gives them (R1). */
class ItemServiceTest {

    static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-10-24T10:00:00Z");
    private static final String MY_ITEMS = "FROM regalos_items WHERE owner_user_id = ? AND status = ?";
    private static final String MY_ITEM = "FROM regalos_items WHERE owner_user_id = ? AND id = ?";
    private static final String INSERT = "INSERT INTO regalos_items";
    private static final String UPDATE = "UPDATE regalos_items SET title = ?";
    private static final String MARK_EDITED = "UPDATE regalos_claims SET item_edited_at = now()";
    private static final String SET_STATUS = "UPDATE regalos_items SET status = ?";
    private static final String SOFT_DELETE = "UPDATE regalos_items SET status = 'DELETED'";
    private static final String HARD_DELETE = "DELETE FROM regalos_items";
    private static final int CRIS = 2;

    private RegalosFakeJdbc jdbc;
    private ItemService service;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        service = new ItemService(new ItemDao(jdbc));
    }

    static Map<String, Object> item(long id, String title, String status) {
        return row("id", id, "title", title, "url", "https://www.amazon.es/dp/1", "store", "amazon.es",
                "price_cents", 3450, "image_url", null, "notes", null, "wanted_a_lot", false, "status", status,
                "created_at", CREATED, "updated_at", CREATED);
    }

    @Test
    void myLetterListsMyActiveWishesWithoutTouchingTheClaims() {
        jdbc.rows(MY_ITEMS, item(42, "Kit de bonsái", "ACTIVE"));

        assertThat(service.list(CRIS, null)).extracting(OwnItem::title).containsExactly("Kit de bonsái");
        assertThat(jdbc.callsWith(MY_ITEMS).get(0).args()).containsExactly(CRIS, "ACTIVE");
        assertThat(jdbc.callsWith("regalos_claims")).isEmpty();
    }

    @Test
    void theReceivedTabListsTheReceivedOnesAndDeletedOnesAreNeverListed() {
        service.list(CRIS, ItemStatus.RECEIVED);
        assertThat(jdbc.callsWith(MY_ITEMS).get(0).args()).containsExactly(CRIS, "RECEIVED");

        assertRegalosError(() -> service.list(CRIS, ItemStatus.DELETED), HttpStatus.BAD_REQUEST, "bad_request");
    }

    @Test
    void createStoresTheValidatedWishForMe() {
        jdbc.scalar(INSERT, 42L).rows(MY_ITEM, item(42, "Sony", "ACTIVE"));

        OwnItem created = service.create(CRIS, request(" Sony ", "https://www.amazon.es/dp/1"));

        assertThat(jdbc.callsWith(INSERT).get(0).args())
                .containsExactly(CRIS, "Sony", "https://www.amazon.es/dp/1", "amazon.es", null, null, null, false);
        assertThat(created.id()).isEqualTo(42L);
        assertThat(created.createdAt()).isEqualTo(CREATED.toInstant());
    }

    /** R6: whoever gives it learns it changed; the owner learns nothing (same answer either way). */
    @Test
    void editingLeavesTheEditedNoticeOnAnyClaim() {
        jdbc.rows(MY_ITEM, item(42, "Sony negro", "ACTIVE"));

        service.update(CRIS, 42, request("Sony negro", null));

        assertThat(jdbc.callsWith(UPDATE).get(0).args()).endsWith(42L, CRIS);
        assertThat(jdbc.callsWith(UPDATE).get(0).sql()).contains("status = 'ACTIVE'");
        assertThat(jdbc.callsWith(MARK_EDITED).get(0).args()).containsExactly(42L);
    }

    @Test
    void editingSomeoneElsesOrAMissingWishIsNotFound() {
        jdbc.updateCount(UPDATE, 0);

        assertRegalosError(() -> service.update(CRIS, 42, request("Sony", null)), HttpStatus.NOT_FOUND, "not_found");
        assertThat(jdbc.callsWith(MARK_EDITED)).isEmpty();
    }

    @Test
    void receivedMovesItFromActiveAndBack() {
        jdbc.rows(MY_ITEM, item(42, "Sony", "RECEIVED"));

        OwnItem received = service.received(CRIS, 42);

        assertThat(jdbc.callsWith(SET_STATUS).get(0).args()).containsExactly("RECEIVED", 42L, CRIS, "ACTIVE");
        assertThat(received.status()).isEqualTo(ItemStatus.RECEIVED);

        jdbc.rows(MY_ITEM, item(42, "Sony", "ACTIVE"));
        service.notReceived(CRIS, 42);
        assertThat(jdbc.callsWith(SET_STATUS).get(1).args()).containsExactly("ACTIVE", 42L, CRIS, "RECEIVED");
    }

    @Test
    void receivedTwiceIsFineButSomeoneElsesIsNotFound() {
        jdbc.updateCount(SET_STATUS, 0).rows(MY_ITEM, item(42, "Sony", "RECEIVED"));
        assertThat(service.received(CRIS, 42).status()).isEqualTo(ItemStatus.RECEIVED);

        jdbc.rows(MY_ITEM);
        jdbc = new RegalosFakeJdbc().updateCount(SET_STATUS, 0);
        service = new ItemService(new ItemDao(jdbc));
        assertRegalosError(() -> service.received(CRIS, 42), HttpStatus.NOT_FOUND, "not_found");
    }

    /** R5: with a claim the row stays (DELETED) so the giver hears of it; without one it goes. */
    @Test
    void deletingAClaimedWishKeepsItAsDeleted() {
        service.delete(CRIS, 42);

        assertThat(jdbc.callsWith(SOFT_DELETE).get(0).args()).containsExactly(42L, CRIS);
        assertThat(jdbc.callsWith(SOFT_DELETE).get(0).sql()).contains("AND EXISTS (SELECT 1 FROM regalos_claims");
        assertThat(jdbc.callsWith(HARD_DELETE)).isEmpty();
    }

    @Test
    void deletingAnUnclaimedWishRemovesIt() {
        jdbc.updateCount(SOFT_DELETE, 0);

        service.delete(CRIS, 42);

        assertThat(jdbc.callsWith(HARD_DELETE).get(0).args()).containsExactly(42L, CRIS);
        assertThat(jdbc.callsWith(HARD_DELETE).get(0).sql()).contains("AND NOT EXISTS (SELECT 1 FROM regalos_claims");
    }

    @Test
    void deletingSomeoneElsesOrAMissingWishIsNotFound() {
        jdbc.updateCount(SOFT_DELETE, 0).updateCount(HARD_DELETE, 0);

        assertRegalosError(() -> service.delete(CRIS, 42), HttpStatus.NOT_FOUND, "not_found");
    }
}
