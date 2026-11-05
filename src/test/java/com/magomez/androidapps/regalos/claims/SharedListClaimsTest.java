package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.config.RegalosConfig;
import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.ideas.IdeaDao;
import com.magomez.androidapps.regalos.lists.ListDao;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.people.RegalosUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static com.magomez.androidapps.regalos.photos.FakePhotoStore.photoFiles;
import static org.assertj.core.api.Assertions.assertThat;

/** Claims on the wishes of a shared list (R8, R9), and lists and ideas in my gifts. */
class SharedListClaimsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), RegalosConfig.SPAIN);
    private static final String ITEM_BY_ID = "WHERE i.id = ?";
    private static final String IS_MEMBER = "FROM regalos_list_members WHERE list_id = ? AND user_id = ?";
    private static final String USER_BY_ID = "FROM regalos_users WHERE id = ?";
    private static final String MY_CLAIMS = "WHERE c.giver_user_id = ? OR (c.giver_user_id IS NULL";
    private static final String MY_IDEAS = "WHERE d.for_user_id <> ? AND (d.author_user_id = ? OR c.user_id = ?)";
    private static final String INSERT = "INSERT INTO regalos_claims";
    private static final String DELETE = "DELETE FROM regalos_claims";
    private static final int CRIS = 1;
    private static final int JAVI = 2;
    private static final int MARIBEL = 5;
    private static final long LEO = 10;

    private RegalosFakeJdbc jdbc;
    private ClaimService service;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        service = new ClaimService(new ClaimDao(jdbc), new PeopleService(new RegalosUserDao(jdbc), CLOCK),
                new ListDao(jdbc), new IdeaDao(jdbc), photoFiles(jdbc));
    }

    /** A wish of Leo's list (birthday 9/11), written by Javi, with no claim. */
    private static Map<String, Object> listWish(long id, String title, Integer price) {
        Map<String, Object> row = new HashMap<>(row("id", id, "owner_user_id", null, "owner_name", null,
                "owner_birthday_day", null, "owner_birthday_month", null, "list_id", LEO, "list_name", "Leo",
                "list_birthday_day", 9, "list_birthday_month", 11, "added_by_name", "Javi"));
        row.putAll(row("title", title, "url", null, "store", null, "price_cents", price, "image_url", null, "photo_updated_at", null,
                "notes", null, "wanted_a_lot", false, "status", "ACTIVE", "claim_state", null, "marked_by_user_id", null,
                "giver_user_id", null, "giver_name", null, "item_edited_at", null));
        return row;
    }

    private static Map<String, Object> myGiftIdea(long id, String title, int price, boolean bought, int authorId,
                                                  String authorName, String claimState, Integer claimUser) {
        Map<String, Object> idea = new HashMap<>(row("id", id, "for_user_id", JAVI, "title", title, "url", null,
                "store", null, "price_cents", price, "notes", null, "bought", bought,
                "created_at", OffsetDateTime.parse("2026-10-30T10:00:00Z"), "photo_updated_at", null));
        idea.putAll(row("author_user_id", authorId, "author_name", authorName, "claim_state", claimState,
                "claim_user_id", claimUser, "claim_user_name", claimUser == null ? null : "Alguien",
                "for_name", "Javi", "for_birthday_day", 3, "for_birthday_month", 2));
        return idea;
    }

    private static Map<String, Object> claimedByMe(Map<String, Object> wish, String state) {
        Map<String, Object> row = new HashMap<>(wish);
        row.putAll(row("claim_state", state, "marked_by_user_id", CRIS, "giver_user_id", CRIS, "giver_name", "Cris"));
        return row;
    }

    private static ClaimRequest me(ClaimState state) {
        return new ClaimRequest(state, new GiverRequest(GiverType.ME, null, null));
    }

    @Test
    void outsidersCannotClaimOrReleaseAListWish() {
        jdbc.rows(ITEM_BY_ID, claimedByMe(listWish(50, "Cubos", 1890), "RESERVED")).scalar(IS_MEMBER, 0);

        assertRegalosError(() -> service.claim(CRIS, 50, me(ClaimState.RESERVED)), HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.unclaim(CRIS, 50), HttpStatus.NOT_FOUND, "not_found");
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
        assertThat(jdbc.callsWith(DELETE)).isEmpty();
    }

    @Test
    void membersClaimAListWishThatHasNoOwner() {
        jdbc.rows(ITEM_BY_ID, listWish(50, "Cubos", 1890)).scalar(IS_MEMBER, 1);

        OthersItem item = service.claim(CRIS, 50, me(ClaimState.RESERVED));

        assertThat(jdbc.callsWith(INSERT).get(0).args()).containsExactly(50L, CRIS, CRIS, null, "RESERVED");
        assertThat(jdbc.callsWith(IS_MEMBER).get(0).args()).containsExactly(LEO, CRIS);
        assertThat(item.addedBy()).isEqualTo("Javi");
    }

    @Test
    void aListWishIsOnlyAssignedToSomeoneInvited() {
        jdbc.rows(ITEM_BY_ID, listWish(50, "Cubos", 1890))
                .rows(USER_BY_ID, row("id", MARIBEL, "username", "maribel", "display_name", "Maribel",
                        "birthday_day", null, "birthday_month", null));
        // The fake answers every membership check the same: invited (to let me in) and Maribel too.
        jdbc.scalar(IS_MEMBER, 1);
        service.claim(CRIS, 50, new ClaimRequest(ClaimState.RESERVED, new GiverRequest(GiverType.USER, MARIBEL, null)));
        assertThat(jdbc.callsWith(INSERT)).hasSize(1);

        jdbc = new RegalosFakeJdbc();
        service = new ClaimService(new ClaimDao(jdbc), new PeopleService(new RegalosUserDao(jdbc), CLOCK),
                new ListDao(jdbc) {
                    @Override
                    public boolean isMember(long listId, int userId) {
                        return userId == CRIS;
                    }
                }, new IdeaDao(jdbc), photoFiles(jdbc));
        jdbc.rows(ITEM_BY_ID, listWish(50, "Cubos", 1890))
                .rows(USER_BY_ID, row("id", MARIBEL, "username", "maribel", "display_name", "Maribel",
                        "birthday_day", null, "birthday_month", null));
        assertRegalosError(() -> service.claim(CRIS, 50,
                        new ClaimRequest(ClaimState.RESERVED, new GiverRequest(GiverType.USER, MARIBEL, null))),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
    }

    @Test
    void myGiftsGroupListsAndPeopleAndAddMyBoughtIdeas() {
        jdbc.rows(MY_CLAIMS, claimedByMe(listWish(50, "Cubos", 1890), "BOUGHT"))
                .rows(MY_IDEAS,
                        myGiftIdea(7, "Gorra", 2200, false, CRIS, "Cris", null, null),
                        myGiftIdea(8, "Cena", 6000, true, CRIS, "Cris", null, null),
                        // Shared with me by Maribel, and I give it: it counts like a claimed wish
                        myGiftIdea(9, "Libro", 1000, false, MARIBEL, "Maribel", "RESERVED", CRIS),
                        // Mine, but Javi's other friend gives it: theirs to count, not mine
                        myGiftIdea(10, "Taza", 500, false, CRIS, "Cris", "BOUGHT", MARIBEL));

        MyClaims mine = service.myClaims(CRIS);

        assertThat(jdbc.callsWith(MY_IDEAS).get(0).args()).containsExactly(CRIS, CRIS, CRIS);
        assertThat(mine.spentCents()).isEqualTo(1890 + 6000);
        assertThat(mine.pendingCents()).isEqualTo(1000);
        assertThat(mine.boughtCount()).isEqualTo(2);
        assertThat(mine.totalCount()).isEqualTo(3);
        assertThat(mine.groups()).hasSize(2);
        ClaimGroup leo = mine.groups().get(0);
        assertThat(leo.list()).isEqualTo(new SharedListRef(LEO, "Leo", new com.magomez.androidapps.regalos.people.Birthday(9, 11), 10));
        assertThat(leo.person()).isNull();
        assertThat(leo.claims()).extracting(MyClaim::title).containsExactly("Cubos");
        ClaimGroup javi = mine.groups().get(1);
        assertThat(javi.person().displayName()).isEqualTo("Javi");
        assertThat(javi.claims()).isEmpty();
        assertThat(javi.ideas()).extracting(idea -> idea.title()).containsExactly("Gorra", "Cena", "Libro", "Taza");
    }
}
