package com.magomez.androidapps.regalos.lists;

import com.magomez.androidapps.regalos.claims.ClaimDao;
import com.magomez.androidapps.regalos.claims.ClaimService;
import com.magomez.androidapps.regalos.config.RegalosConfig;
import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.ideas.IdeaDao;
import com.magomez.androidapps.regalos.items.ItemRequest;
import com.magomez.androidapps.regalos.people.Birthday;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.people.RegalosUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lists for someone outside the app: only their members see them (R8), anyone of them adds,
 * edits and seals wishes, and only whoever created one renames it, invites or deletes it.
 */
class ListServiceTest {

    /** 30th of October in Madrid. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), RegalosConfig.SPAIN);
    private static final String SUMMARIES = "FROM regalos_lists l JOIN regalos_list_members m";
    private static final String IS_MEMBER = "FROM regalos_list_members WHERE list_id = ? AND user_id = ?";
    private static final String MEMBERS = "WHERE m.list_id = ? ORDER BY";
    private static final String USER_BY_ID = "FROM regalos_users WHERE id = ?";
    private static final String INSERT_LIST = "INSERT INTO regalos_lists";
    private static final String INSERT_MEMBER = "INSERT INTO regalos_list_members";
    private static final String DELETE_MEMBER = "DELETE FROM regalos_list_members";
    private static final String DELETE_CLAIMS = "DELETE FROM regalos_claims c USING regalos_items i";
    private static final String UPDATE_LIST = "UPDATE regalos_lists SET name = ?";
    private static final String DELETE_LIST = "DELETE FROM regalos_lists";
    private static final String INSERT_ITEM = "INSERT INTO regalos_items (list_id, added_by_user_id";
    private static final String ITEM_BY_ID = "WHERE i.id = ?";
    private static final String LIST_ITEMS = "WHERE i.list_id = ? AND i.status = 'ACTIVE'";
    private static final String SOFT_DELETE = "UPDATE regalos_items SET status = 'DELETED'";
    private static final String HARD_DELETE = "DELETE FROM regalos_items";
    private static final int CRIS = 1;
    private static final int JAVI = 2;
    private static final int ALBA = 3;
    private static final int DAVID = 4;
    private static final long LEO = 10;

    private RegalosFakeJdbc jdbc;
    private ListService service;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        PeopleService people = new PeopleService(new RegalosUserDao(jdbc), CLOCK);
        ListDao listDao = new ListDao(jdbc);
        ClaimService claims = new ClaimService(new ClaimDao(jdbc), people, listDao, new IdeaDao(jdbc));
        service = new ListService(listDao, claims, people);
    }

    private static Map<String, Object> summary(long id, String name, Integer day, Integer month, int creator) {
        return row("id", id, "name", name, "birthday_day", day, "birthday_month", month,
                "created_by_user_id", creator, "created_by_name", creator == JAVI ? "Javi" : "Cris",
                "member_count", 4L, "wish_count", 5L, "free_count", 2L, "mine_count", 1L);
    }

    private static Map<String, Object> user(int id, String name) {
        return row("id", id, "username", name.toLowerCase(), "display_name", name,
                "birthday_day", null, "birthday_month", null);
    }

    private void member(boolean member) {
        jdbc.scalar(IS_MEMBER, member ? 1 : 0);
    }

    private void leoOfJavi() {
        member(true);
        jdbc.rows(SUMMARIES, summary(LEO, "Leo", 9, 11, JAVI));
    }

    @Test
    void myListsAreTheOnesImInvitedTo() {
        jdbc.rows(SUMMARIES, summary(LEO, "Leo", 9, 11, JAVI), summary(11, "La abuela", null, null, CRIS));

        List<ListSummary> lists = service.lists(CRIS);

        assertThat(lists).extracting(ListSummary::name).containsExactly("Leo", "La abuela");
        ListSummary leo = lists.get(0);
        assertThat(leo.daysUntilBirthday()).isEqualTo(10);
        assertThat(leo.createdBy()).isEqualTo(new ListCreator(JAVI, "Javi"));
        assertThat(leo.canManage()).isFalse();
        assertThat(lists.get(1).canManage()).isTrue();
        assertThat(jdbc.callsWith(SUMMARIES).get(0).sql()).contains("m.user_id = ?");
    }

    @Test
    void createInvitesTheChosenPeopleAndMe() {
        jdbc.rows(USER_BY_ID, user(JAVI, "Javi")).scalar(INSERT_LIST, LEO);
        leoOfJavi();

        service.create(CRIS, new ListRequest("  Leo ", new Birthday(9, 11), List.of(JAVI, JAVI, CRIS)));

        assertThat(jdbc.callsWith(INSERT_LIST).get(0).args()).containsExactly("Leo", 9, 11, CRIS);
        assertThat(jdbc.callsWith(INSERT_MEMBER)).extracting(call -> call.args()[1]).containsExactlyInAnyOrder(CRIS, JAVI);
    }

    @Test
    void createRejectsABadNameBirthdayOrPerson() {
        assertRegalosError(() -> service.create(CRIS, new ListRequest(" ", null, List.of())),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.create(CRIS, new ListRequest("x".repeat(61), null, List.of())),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.create(CRIS, new ListRequest("Leo", new Birthday(31, 4), List.of())),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.create(CRIS, new ListRequest("Leo", null, List.of(99))),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.callsWith(INSERT_LIST)).isEmpty();
    }

    /** R8: to someone not invited, a list does not exist. */
    @Test
    void outsidersGetNotFoundForEverything() {
        member(false);
        ItemRequest wish = new ItemRequest("Cubos", null, null, null, null, null, null);

        assertRegalosError(() -> service.detail(CRIS, LEO), HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.addItem(CRIS, LEO, wish), HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.updateItem(CRIS, LEO, 5, wish), HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.deleteItem(CRIS, LEO, 5), HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.leave(CRIS, LEO), HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.delete(CRIS, LEO), HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.update(CRIS, LEO, new ListRequest("Leo", null, List.of())),
                HttpStatus.NOT_FOUND, "not_found");
        assertThat(jdbc.callsWith(LIST_ITEMS)).isEmpty();
        assertThat(jdbc.callsWith(INSERT_ITEM)).isEmpty();
    }

    @Test
    void membersSeeTheMembersAndTheWishesWithTheirClaims() {
        leoOfJavi();
        jdbc.rows(MEMBERS, user(ALBA, "Alba"), user(JAVI, "Javi"));

        ListDetail detail = service.detail(CRIS, LEO);

        assertThat(detail.list().name()).isEqualTo("Leo");
        assertThat(detail.members()).extracting(member -> member.displayName()).containsExactly("Alba", "Javi");
        assertThat(jdbc.callsWith(LIST_ITEMS).get(0).args()).containsExactly(LEO);
    }

    @Test
    void onlyTheCreatorRenamesInvitesOrDeletes() {
        leoOfJavi();

        assertRegalosError(() -> service.update(CRIS, LEO, new ListRequest("Leo", null, List.of())),
                HttpStatus.FORBIDDEN, "forbidden");
        assertRegalosError(() -> service.delete(CRIS, LEO), HttpStatus.FORBIDDEN, "forbidden");
        assertThat(jdbc.callsWith(UPDATE_LIST)).isEmpty();
        assertThat(jdbc.callsWith(DELETE_LIST)).isEmpty();
    }

    @Test
    void updatingTheMembersDropsTheLeaversAndTheirClaims() {
        member(true);
        jdbc.rows(SUMMARIES, summary(LEO, "Leo", 9, 11, CRIS))
                .rows(MEMBERS, user(CRIS, "Cris"), user(JAVI, "Javi"), user(ALBA, "Alba"))
                .rows(USER_BY_ID, user(DAVID, "David"));

        service.update(CRIS, LEO, new ListRequest("Leo", null, List.of(JAVI, DAVID)));

        assertThat(jdbc.callsWith(UPDATE_LIST).get(0).args()).containsExactly("Leo", null, null, LEO);
        assertThat(jdbc.callsWith(DELETE_MEMBER)).extracting(call -> call.args()[1]).containsExactly(ALBA);
        assertThat(jdbc.callsWith(DELETE_CLAIMS).get(0).args()).containsExactly(LEO, ALBA, ALBA);
        assertThat(jdbc.callsWith(INSERT_MEMBER)).extracting(call -> call.args()[1]).containsExactly(DAVID);
    }

    @Test
    void theCreatorDeletesTheList() {
        member(true);
        jdbc.rows(SUMMARIES, summary(LEO, "Leo", 9, 11, CRIS));

        service.delete(CRIS, LEO);

        assertThat(jdbc.callsWith(DELETE_LIST).get(0).args()).containsExactly(LEO);
    }

    @Test
    void membersCanLeaveButTheCreatorCannot() {
        leoOfJavi();
        service.leave(CRIS, LEO);
        assertThat(jdbc.callsWith(DELETE_CLAIMS).get(0).args()).containsExactly(LEO, CRIS, CRIS);
        assertThat(jdbc.callsWith(DELETE_MEMBER).get(0).args()).containsExactly(LEO, CRIS);

        assertRegalosError(() -> service.leave(JAVI, LEO), HttpStatus.BAD_REQUEST, "bad_request");
    }

    @Test
    void anyMemberAddsAWishSignedWithTheirName() {
        leoOfJavi();
        jdbc.scalar(INSERT_ITEM, 50L).rows(ITEM_BY_ID, row("id", 50L, "owner_user_id", null, "list_id", LEO,
                "title", "Cubos", "url", null, "store", null, "price_cents", 1890, "image_url", null, "notes", null,
                "wanted_a_lot", false, "status", "ACTIVE", "owner_name", null, "owner_birthday_day", null,
                "owner_birthday_month", null, "list_name", "Leo", "list_birthday_day", 9, "list_birthday_month", 11,
                "added_by_name", "Cris", "claim_state", null, "marked_by_user_id", null, "giver_user_id", null,
                "giver_name", null, "item_edited_at", null));

        var item = service.addItem(CRIS, LEO, new ItemRequest(" Cubos ", null, null, 1890, null, null, null));

        assertThat(jdbc.callsWith(INSERT_ITEM).get(0).args()).startsWith(LEO, CRIS, "Cubos");
        assertThat(item.addedBy()).isEqualTo("Cris");
    }

    @Test
    void deletingAListWishKeepsItWhenSomeoneClaimedIt() {
        leoOfJavi();
        service.deleteItem(CRIS, LEO, 50);
        assertThat(jdbc.callsWith(SOFT_DELETE).get(0).args()).containsExactly(50L, LEO);

        jdbc.updateCount(SOFT_DELETE, 0).updateCount(HARD_DELETE, 0);
        assertRegalosError(() -> service.deleteItem(CRIS, LEO, 50), HttpStatus.NOT_FOUND, "not_found");
    }
}
