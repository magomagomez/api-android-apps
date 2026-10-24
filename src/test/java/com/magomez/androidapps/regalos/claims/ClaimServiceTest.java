package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.config.RegalosConfig;
import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.people.Birthday;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.people.Person;
import com.magomez.androidapps.regalos.people.RegalosUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Giving: someone else's letter, claiming and releasing a wish, and my gifts. The rules of
 * the contract: R1 (the owner sees nothing), R2 (one claim per wish), R3 (givers see who),
 * R4 (only whoever claimed it or was assigned it changes it).
 */
class ClaimServiceTest {

    /** 24th of October in Madrid. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T08:00:00Z"), RegalosConfig.SPAIN);
    private static final String ITEMS_OF = "WHERE i.owner_user_id = ? AND i.status = 'ACTIVE'";
    private static final String ITEM_BY_ID = "WHERE i.id = ?";
    private static final String MY_CLAIMS = "WHERE c.giver_user_id = ? OR (c.giver_user_id IS NULL";
    private static final String USER_BY_ID = "FROM regalos_users WHERE id = ?";
    private static final String INSERT = "INSERT INTO regalos_claims";
    private static final String UPDATE = "UPDATE regalos_claims SET giver_user_id = ?";
    private static final String DELETE = "DELETE FROM regalos_claims";
    private static final String PURGE = "DELETE FROM regalos_items";
    private static final int MAGOMEZ = 1;
    private static final int CRIS = 2;
    private static final int ALEX = 3;
    private static final int SARA = 4;

    private RegalosFakeJdbc jdbc;
    private ClaimService service;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        service = new ClaimService(new ClaimDao(jdbc), new PeopleService(new RegalosUserDao(jdbc), CLOCK));
    }

    /** A wish of Cris (birthday 12/11) with no claim. */
    private static Map<String, Object> wish(long id, String title, Integer priceCents) {
        Map<String, Object> row = new HashMap<>(row("id", id, "owner_user_id", CRIS, "title", title,
                "url", null, "store", null, "price_cents", priceCents, "image_url", null, "notes", null,
                "wanted_a_lot", false, "status", "ACTIVE",
                "owner_name", "Cris", "owner_birthday_day", 12, "owner_birthday_month", 11));
        row.putAll(row("claim_state", null, "marked_by_user_id", null, "giver_user_id", null, "giver_name", null,
                "item_edited_at", null));
        return row;
    }

    private static Map<String, Object> claimed(Map<String, Object> wish, String state, int markedBy,
                                               Integer giverId, String giverName) {
        Map<String, Object> row = new HashMap<>(wish);
        row.putAll(row("claim_state", state, "marked_by_user_id", markedBy, "giver_user_id", giverId,
                "giver_name", giverName));
        return row;
    }

    private static Map<String, Object> with(Map<String, Object> wish, String column, Object value) {
        Map<String, Object> row = new HashMap<>(wish);
        row.put(column, value);
        return row;
    }

    private static Map<String, Object> user(int id, String name) {
        return row("id", id, "username", name.toLowerCase(), "display_name", name,
                "birthday_day", null, "birthday_month", null);
    }

    private static ClaimRequest me(ClaimState state) {
        return new ClaimRequest(state, new GiverRequest(GiverType.ME, null, null));
    }

    // R1 ---------------------------------------------------------------------------------

    @Test
    void myOwnLetterIsForbiddenHere() {
        assertRegalosError(() -> service.listOf(CRIS, CRIS), HttpStatus.FORBIDDEN, "forbidden");
        assertThat(jdbc.calls).isEmpty();
    }

    /** Same 403 whether or not someone claimed it: the answer cannot tell the owner anything. */
    @Test
    void theOwnerCannotClaimOrReleaseTheirOwnWishWhateverItsClaim() {
        for (Map<String, Object> row : new Map[]{wish(42, "Bonsái", 3450),
                claimed(wish(42, "Bonsái", 3450), "BOUGHT", ALEX, ALEX, "Alex")}) {
            jdbc.rows(ITEM_BY_ID, row);
            RegalosException put = assertRegalosError(() -> service.claim(CRIS, 42, me(ClaimState.RESERVED)),
                    HttpStatus.FORBIDDEN, "forbidden");
            RegalosException delete = assertRegalosError(() -> service.unclaim(CRIS, 42),
                    HttpStatus.FORBIDDEN, "forbidden");
            assertThat(put.getMessage()).isEqualTo("Es un deseo tuyo");
            assertThat(delete.getMessage()).isEqualTo("Es un deseo tuyo");
        }
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
        assertThat(jdbc.callsWith(DELETE)).isEmpty();
    }

    @Test
    void nobodyCanAssignAWishToItsOwner() {
        jdbc.rows(ITEM_BY_ID, wish(42, "Bonsái", 3450)).rows(USER_BY_ID, user(CRIS, "Cris"));

        assertRegalosError(() -> service.claim(MAGOMEZ, 42,
                        new ClaimRequest(ClaimState.RESERVED, new GiverRequest(GiverType.USER, CRIS, null))),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
    }

    // Someone else's letter (R3) ----------------------------------------------------------

    @Test
    void someoneElsesLetterShowsWhoGivesEachWish() {
        jdbc.rows(USER_BY_ID, row("id", CRIS, "username", "cris", "display_name", "Cris",
                        "birthday_day", 12, "birthday_month", 11))
                .rows(ITEMS_OF, wish(41, "Chaqueta", 22000),
                        claimed(wish(42, "Bonsái", 3450), "RESERVED", MAGOMEZ, MAGOMEZ, "Magomez"),
                        claimed(wish(43, "Entradas", 9000), "BOUGHT", ALEX, ALEX, "Alex"),
                        claimed(wish(44, "Bufanda", 4500), "RESERVED", ALEX, null, "la abuela"));

        PersonItems letter = service.listOf(MAGOMEZ, CRIS);

        assertThat(letter.person()).isEqualTo(new Person(CRIS, "Cris", new Birthday(12, 11), 19));
        assertThat(jdbc.callsWith(ITEMS_OF).get(0).args()).containsExactly(CRIS);
        assertThat(letter.items()).extracting(OthersItem::claim).containsExactly(
                null,
                new Claim(ClaimState.RESERVED, new Giver(MAGOMEZ, "Magomez"), true, true),
                new Claim(ClaimState.BOUGHT, new Giver(ALEX, "Alex"), false, false),
                new Claim(ClaimState.RESERVED, new Giver(null, "la abuela"), false, false));
    }

    @Test
    void theLetterOfAnUnknownPersonIsNotFound() {
        assertRegalosError(() -> service.listOf(MAGOMEZ, 99), HttpStatus.NOT_FOUND, "not_found");
    }

    // Claiming (R2) -----------------------------------------------------------------------

    @Test
    void claimingAFreeWishForMyself() {
        jdbc.rows(ITEM_BY_ID, wish(42, "Bonsái", 3450));

        service.claim(MAGOMEZ, 42, me(ClaimState.RESERVED));

        assertThat(jdbc.callsWith(INSERT).get(0).args()).containsExactly(42L, MAGOMEZ, MAGOMEZ, null, "RESERVED");
        assertThat(jdbc.callsWith(INSERT).get(0).sql()).contains("ON CONFLICT (item_id) DO NOTHING");
    }

    @Test
    void claimingForAnotherUserOrForSomeoneOutsideTheApp() {
        jdbc.rows(ITEM_BY_ID, wish(42, "Bonsái", 3450)).rows(USER_BY_ID, user(SARA, "Sara"));

        service.claim(MAGOMEZ, 42, new ClaimRequest(ClaimState.RESERVED, new GiverRequest(GiverType.USER, SARA, null)));
        service.claim(MAGOMEZ, 42, new ClaimRequest(ClaimState.BOUGHT,
                new GiverRequest(GiverType.EXTERNAL, null, "  la abuela ")));

        assertThat(jdbc.callsWith(INSERT).get(0).args()).containsExactly(42L, MAGOMEZ, SARA, null, "RESERVED");
        assertThat(jdbc.callsWith(INSERT).get(1).args()).containsExactly(42L, MAGOMEZ, null, "la abuela", "BOUGHT");
    }

    @Test
    void assigningItToMyselfAsAUserIsTheSameAsMe() {
        jdbc.rows(ITEM_BY_ID, wish(42, "Bonsái", 3450));

        service.claim(MAGOMEZ, 42, new ClaimRequest(ClaimState.BOUGHT,
                new GiverRequest(GiverType.USER, MAGOMEZ, null)));

        assertThat(jdbc.callsWith(INSERT).get(0).args()).containsExactly(42L, MAGOMEZ, MAGOMEZ, null, "BOUGHT");
    }

    @Test
    void badClaimsAreRejected() {
        jdbc.rows(ITEM_BY_ID, wish(42, "Bonsái", 3450));
        ClaimRequest[] bad = {null, new ClaimRequest(null, new GiverRequest(GiverType.ME, null, null)),
                new ClaimRequest(ClaimState.RESERVED, null),
                new ClaimRequest(ClaimState.RESERVED, new GiverRequest(null, null, null)),
                new ClaimRequest(ClaimState.RESERVED, new GiverRequest(GiverType.USER, null, null)),
                new ClaimRequest(ClaimState.RESERVED, new GiverRequest(GiverType.USER, 99, null)),
                new ClaimRequest(ClaimState.RESERVED, new GiverRequest(GiverType.EXTERNAL, null, "  ")),
                new ClaimRequest(ClaimState.RESERVED, new GiverRequest(GiverType.EXTERNAL, null, "x".repeat(61)))};
        for (ClaimRequest request : bad) {
            assertRegalosError(() -> service.claim(MAGOMEZ, 42, request), HttpStatus.BAD_REQUEST, "bad_request");
        }
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
    }

    @Test
    void onlyActiveWishesCanBeClaimed() {
        assertRegalosError(() -> service.claim(MAGOMEZ, 42, me(ClaimState.RESERVED)), HttpStatus.NOT_FOUND, "not_found");
        for (String status : new String[]{"RECEIVED", "DELETED"}) {
            jdbc.rows(ITEM_BY_ID, with(wish(42, "Bonsái", 3450), "status", status));
            assertRegalosError(() -> service.claim(MAGOMEZ, 42, me(ClaimState.RESERVED)),
                    HttpStatus.NOT_FOUND, "not_found");
        }
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
    }

    @Test
    void aWishSomeoneElseClaimedIsAConflictThatSaysWho() {
        jdbc.rows(ITEM_BY_ID, claimed(wish(42, "Bonsái", 3450), "BOUGHT", ALEX, ALEX, "Alex"));

        RegalosException e = assertRegalosError(() -> service.claim(MAGOMEZ, 42, me(ClaimState.RESERVED)),
                HttpStatus.CONFLICT, "already_claimed");

        assertThat(e.getMessage()).isEqualTo("Ya lo regala Alex");
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
        assertThat(jdbc.callsWith(UPDATE)).isEmpty();
    }

    /** R2 under a race: both read "free", only one INSERT wins, the other gets the 409. */
    @Test
    void losingTheRaceForAFreeWishIsAConflict() {
        jdbc.rows(ITEM_BY_ID, wish(42, "Bonsái", 3450)).updateCount(INSERT, 0);

        assertRegalosError(() -> service.claim(MAGOMEZ, 42, me(ClaimState.RESERVED)),
                HttpStatus.CONFLICT, "already_claimed");
    }

    @Test
    void aWishDeletedMeanwhileIsNotFound() {
        jdbc.rows(ITEM_BY_ID, wish(42, "Bonsái", 3450))
                .fail(INSERT, new DataIntegrityViolationException("regalos_claims_item_id_fkey"));

        assertRegalosError(() -> service.claim(MAGOMEZ, 42, me(ClaimState.RESERVED)), HttpStatus.NOT_FOUND, "not_found");
    }

    // Changing and releasing (R4) ----------------------------------------------------------

    @Test
    void whoeverClaimedItCanChangeItAndThatClearsTheEditedNotice() {
        jdbc.rows(ITEM_BY_ID, claimed(wish(42, "Bonsái", 3450), "RESERVED", MAGOMEZ, MAGOMEZ, "Magomez"));

        service.claim(MAGOMEZ, 42, me(ClaimState.BOUGHT));

        assertThat(jdbc.callsWith(UPDATE).get(0).args()).containsExactly(MAGOMEZ, null, "BOUGHT", 42L, MAGOMEZ, MAGOMEZ);
        assertThat(jdbc.callsWith(UPDATE).get(0).sql()).contains("item_edited_at = NULL")
                .contains("AND (marked_by_user_id = ? OR giver_user_id = ?)");
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
    }

    @Test
    void theAssignedGiverCanChangeItToo() {
        jdbc.rows(ITEM_BY_ID, claimed(wish(42, "Bonsái", 3450), "RESERVED", MAGOMEZ, SARA, "Sara"));

        service.claim(SARA, 42, me(ClaimState.BOUGHT));

        assertThat(jdbc.callsWith(UPDATE).get(0).args()).containsExactly(SARA, null, "BOUGHT", 42L, SARA, SARA);
    }

    @Test
    void releasingMyClaim() {
        jdbc.rows(ITEM_BY_ID, claimed(wish(42, "Bonsái", 3450), "RESERVED", MAGOMEZ, MAGOMEZ, "Magomez"));

        service.unclaim(MAGOMEZ, 42);

        assertThat(jdbc.callsWith(DELETE).get(0).args()).containsExactly(42L, MAGOMEZ, MAGOMEZ);
        assertThat(jdbc.callsWith(PURGE)).isEmpty();
    }

    @Test
    void someoneElsesClaimCannotBeReleased() {
        jdbc.rows(ITEM_BY_ID, claimed(wish(42, "Bonsái", 3450), "RESERVED", ALEX, null, "la abuela"));

        assertRegalosError(() -> service.unclaim(MAGOMEZ, 42), HttpStatus.FORBIDDEN, "forbidden");
        assertThat(jdbc.callsWith(DELETE)).isEmpty();
    }

    @Test
    void releasingWithoutAClaimIsNotFound() {
        jdbc.rows(ITEM_BY_ID, wish(42, "Bonsái", 3450));
        assertRegalosError(() -> service.unclaim(MAGOMEZ, 42), HttpStatus.NOT_FOUND, "not_found");

        jdbc.rows(ITEM_BY_ID);
        jdbc = new RegalosFakeJdbc();
        service = new ClaimService(new ClaimDao(jdbc), new PeopleService(new RegalosUserDao(jdbc), CLOCK));
        assertRegalosError(() -> service.unclaim(MAGOMEZ, 42), HttpStatus.NOT_FOUND, "not_found");
    }

    /** Dismissing a REMOVED gift: with its last claim gone, the deleted wish goes for good. */
    @Test
    void releasingTheClaimOfADeletedWishPurgesIt() {
        jdbc.rows(ITEM_BY_ID, with(claimed(wish(42, "Funda Kindle", 2400), "BOUGHT", MAGOMEZ, MAGOMEZ, "Magomez"),
                "status", "DELETED"));

        service.unclaim(MAGOMEZ, 42);

        assertThat(jdbc.callsWith(PURGE).get(0).args()).containsExactly(42L);
        assertThat(jdbc.callsWith(PURGE).get(0).sql()).contains("status = 'DELETED'");
    }

    // My gifts ----------------------------------------------------------------------------

    @Test
    void myGiftsAreGroupedByPersonWithTotalsAndNotices() {
        Map<String, Object> sara = row("owner_user_id", SARA, "owner_name", "Sara",
                "owner_birthday_day", 27, "owner_birthday_month", 6);
        Map<String, Object> cascadia = new HashMap<>(claimed(wish(50, "Cascadia", 3990), "BOUGHT", MAGOMEZ, MAGOMEZ,
                "Magomez"));
        cascadia.putAll(sara);
        Map<String, Object> kindle = new HashMap<>(with(claimed(wish(51, "Funda Kindle", 2400), "BOUGHT", MAGOMEZ,
                MAGOMEZ, "Magomez"), "status", "DELETED"));
        kindle.putAll(sara);
        jdbc.rows(MY_CLAIMS,
                cascadia,
                kindle,
                claimed(wish(42, "Bonsái", 3450), "RESERVED", MAGOMEZ, MAGOMEZ, "Magomez"),
                with(claimed(wish(43, "Libro", null), "RESERVED", SARA, MAGOMEZ, "Magomez"),
                        "item_edited_at", OffsetDateTime.parse("2026-10-23T10:00:00Z")),
                claimed(wish(44, "Bufanda", 4500), "RESERVED", MAGOMEZ, null, "la abuela"),
                with(claimed(wish(45, "Vela", 2400), "BOUGHT", MAGOMEZ, MAGOMEZ, "Magomez"), "status", "RECEIVED"));

        MyClaims mine = service.myClaims(MAGOMEZ);

        assertThat(jdbc.callsWith(MY_CLAIMS).get(0).args()).containsExactly(MAGOMEZ, MAGOMEZ);
        assertThat(mine.spentCents()).isEqualTo(3990 + 2400 + 2400);
        assertThat(mine.pendingCents()).isEqualTo(3450);
        assertThat(mine.boughtCount()).isEqualTo(3);
        assertThat(mine.totalCount()).isEqualTo(5);
        assertThat(mine.groups()).extracting(group -> group.person().displayName()).containsExactly("Cris", "Sara");
        ClaimGroup cris = mine.groups().get(0);
        assertThat(cris.person().daysUntilBirthday()).isEqualTo(19);
        assertThat(cris.claims()).extracting(MyClaim::title, MyClaim::notice, MyClaim::mine).containsExactly(
                org.assertj.core.groups.Tuple.tuple("Bonsái", null, true),
                org.assertj.core.groups.Tuple.tuple("Libro", Notice.EDITED, true),
                org.assertj.core.groups.Tuple.tuple("Bufanda", null, false),
                org.assertj.core.groups.Tuple.tuple("Vela", Notice.RECEIVED, true));
        assertThat(mine.groups().get(1).claims()).extracting(MyClaim::notice).containsExactly(null, Notice.REMOVED);
    }

    @Test
    void withoutGiftsMyGiftsAreEmpty() {
        MyClaims mine = service.myClaims(MAGOMEZ);

        assertThat(mine).isEqualTo(new MyClaims(0, 0, 0, 0, java.util.List.of()));
    }
}
