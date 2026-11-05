package com.magomez.androidapps.regalos.ideas;

import com.magomez.androidapps.regalos.claims.ClaimState;
import com.magomez.androidapps.regalos.config.RegalosConfig;
import com.magomez.androidapps.regalos.config.RegalosException;
import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.people.RegalosUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static com.magomez.androidapps.regalos.photos.FakePhotoStore.photoFiles;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ideas for someone else: their author writes and shares them, the people they are shared with
 * see them and may give them, and the person they are for never does (R7, R13, R14).
 */
class IdeaServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), RegalosConfig.SPAIN);
    private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-10-30T10:00:00Z");
    private static final String FOR_PERSON = "WHERE d.for_user_id = ? AND d.for_user_id <> ?";
    private static final String BY_ID = "WHERE d.id = ? AND d.for_user_id <> ?";
    private static final String SHARES_FOR_PERSON = "WHERE d.author_user_id = ? AND d.for_user_id = ? ORDER BY s.idea_id";
    private static final String SHARES_OF = "WHERE d.author_user_id = ? AND s.idea_id = ?";
    private static final String INSERT = "INSERT INTO regalos_ideas";
    private static final String UPDATE = "UPDATE regalos_ideas SET title = ?";
    private static final String DELETE = "DELETE FROM regalos_ideas";
    private static final String INSERT_SHARE = "INSERT INTO regalos_idea_shares";
    private static final String DELETE_SHARE = "DELETE FROM regalos_idea_shares";
    private static final String INSERT_CLAIM = "INSERT INTO regalos_idea_claims";
    private static final String UPDATE_CLAIM = "UPDATE regalos_idea_claims";
    private static final String DELETE_CLAIM = "DELETE FROM regalos_idea_claims";
    private static final String USER_BY_ID = "FROM regalos_users WHERE id = ?";
    private static final int CRIS = 1;
    private static final int JAVI = 2;
    private static final int ALBA = 3;
    private static final int DAVID = 4;
    private static final int MARIBEL = 5;

    private RegalosFakeJdbc jdbc;
    private IdeaService service;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        service = new IdeaService(new IdeaDao(jdbc), new PeopleService(new RegalosUserDao(jdbc), CLOCK), photoFiles(jdbc));
    }

    /** An idea for Javi written by Cris, as the database answers it. */
    static Map<String, Object> idea(long id, String title, boolean bought) {
        return ideaBy(id, title, bought, CRIS, "Cris");
    }

    static Map<String, Object> ideaBy(long id, String title, boolean bought, int authorId, String authorName) {
        return row("id", id, "for_user_id", JAVI, "title", title, "url", null, "store", null, "price_cents", 2200,
                "notes", null, "bought", bought, "created_at", CREATED, "photo_updated_at", null,
                "author_user_id", authorId, "author_name", authorName,
                "claim_state", null, "claim_user_id", null, "claim_user_name", null);
    }

    private static Map<String, Object> claimedBy(Map<String, Object> idea, int userId, String name, String state) {
        Map<String, Object> claimed = new java.util.HashMap<>(idea);
        claimed.putAll(row("claim_state", state, "claim_user_id", userId, "claim_user_name", name));
        return claimed;
    }

    private static Map<String, Object> share(long ideaId, int userId) {
        return row("idea_id", ideaId, "user_id", userId);
    }

    private static IdeaRequest request(String title, Boolean bought, List<Integer> sharedWith) {
        return new IdeaRequest(title, null, null, 2200, null, bought, sharedWith);
    }

    /** Every user id answers as an existing person (the fake does not look at the id). */
    private void peopleExist() {
        jdbc.rows(USER_BY_ID, row("id", JAVI, "username", "javi", "display_name", "Javi",
                "birthday_day", null, "birthday_month", null));
    }

    @Test
    void iSeeMyIdeasWithWhomTheyAreSharedAndTheIdeasSharedWithMe() {
        peopleExist();
        jdbc.rows(FOR_PERSON, idea(7, "Gorra para correr", false), ideaBy(9, "Cena en el italiano", false, ALBA, "Alba"))
                .rows(SHARES_FOR_PERSON, share(7, ALBA), share(7, DAVID));

        List<Idea> ideas = service.list(CRIS, JAVI);

        assertThat(ideas).extracting(Idea::title).containsExactly("Gorra para correr", "Cena en el italiano");
        assertThat(ideas.get(0).mine()).isTrue();
        assertThat(ideas.get(0).sharedWith()).containsExactly(ALBA, DAVID);
        assertThat(ideas.get(1).mine()).isFalse();
        assertThat(ideas.get(1).author()).isEqualTo(new IdeaAuthor(ALBA, "Alba"));
        // Who else sees Alba's idea is hers to know.
        assertThat(ideas.get(1).sharedWith()).isNull();
        assertThat(jdbc.callsWith(FOR_PERSON).get(0).args()).containsExactly(JAVI, CRIS, CRIS, CRIS);
    }

    /** R13 in SQL: whatever the share rows say, an idea never reaches the person it is for. */
    @Test
    void everyReadOfAnIdeaLeavesOutThePersonItIsFor() {
        peopleExist();
        service.list(CRIS, JAVI);
        jdbc.rows(BY_ID, idea(7, "Gorra", false));
        service.claim(ALBA, 7, new IdeaClaimRequest(ClaimState.RESERVED));

        assertThat(jdbc.callsWith(FOR_PERSON).get(0).sql())
                .contains("d.for_user_id <> ? AND (d.author_user_id = ? OR EXISTS (SELECT 1 FROM regalos_idea_shares s");
        assertThat(jdbc.callsWith(BY_ID).get(0).args()).containsExactly(7L, ALBA, ALBA, ALBA);
    }

    @Test
    void ideasForMyselfAreForbidden() {
        assertRegalosError(() -> service.list(CRIS, CRIS), HttpStatus.FORBIDDEN, "forbidden");
        assertRegalosError(() -> service.create(CRIS, CRIS, request("Algo", null, null)), HttpStatus.FORBIDDEN, "forbidden");
        assertThat(jdbc.callsWith("regalos_ideas")).isEmpty();
    }

    @Test
    void ideasForAnUnknownPersonAreNotFound() {
        assertRegalosError(() -> service.list(CRIS, 99), HttpStatus.NOT_FOUND, "not_found");
    }

    @Test
    void createValidatesLikeAWishStoresMeAsAuthorAndSharesItOnce() {
        peopleExist();
        jdbc.scalar(INSERT, 7L).rows(BY_ID, idea(7, "Gorra para correr", false)).rows(SHARES_OF, share(7, ALBA), share(7, DAVID));

        Idea idea = service.create(CRIS, JAVI, new IdeaRequest(" Gorra para correr ", "https://www.decathlon.es/gorra", null,
                2200, null, null, List.of(ALBA, DAVID, ALBA)));

        assertThat(jdbc.callsWith(INSERT).get(0).args())
                .containsExactly(CRIS, JAVI, "Gorra para correr", "https://www.decathlon.es/gorra", "decathlon.es", 2200,
                        null, false);
        assertThat(jdbc.callsWith(INSERT_SHARE)).extracting(call -> List.of(call.args()))
                .containsExactly(List.of(7L, ALBA), List.of(7L, DAVID));
        assertThat(idea.sharedWith()).containsExactly(ALBA, DAVID);
        assertRegalosError(() -> service.create(CRIS, JAVI, request(" ", null, null)), HttpStatus.BAD_REQUEST, "bad_request");
    }

    /** R13: sharing with the person it is for is refused, not just hidden. */
    @Test
    void anIdeaCannotBeSharedWithWhomItIsForNorWithItsAuthor() {
        peopleExist();

        assertRegalosError(() -> service.create(CRIS, JAVI, request("Gorra", null, List.of(ALBA, JAVI))),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.create(CRIS, JAVI, request("Gorra", null, List.of(CRIS))),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
        assertThat(jdbc.callsWith(INSERT_SHARE)).isEmpty();
    }

    @Test
    void sharingWithSomeoneWhoDoesNotExistIsRefused() {
        jdbc.rows(BY_ID, idea(7, "Gorra", false));

        assertRegalosError(() -> service.update(CRIS, 7, request("Gorra", null, List.of(99))),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.callsWith(UPDATE)).isEmpty();
    }

    @Test
    void whoeverStopsSeeingAnIdeaLosesTheirClaimOnIt() {
        peopleExist();
        jdbc.rows(BY_ID, idea(7, "Gorra", false)).rows(SHARES_OF, share(7, ALBA), share(7, DAVID));

        service.update(CRIS, 7, request("Gorra", false, List.of(DAVID, MARIBEL)));

        assertThat(jdbc.callsWith(DELETE_CLAIM).get(0).args()).containsExactly(7L, ALBA);
        assertThat(jdbc.callsWith(DELETE_SHARE).get(0).args()).containsExactly(7L, ALBA);
        assertThat(jdbc.callsWith(INSERT_SHARE)).singleElement().extracting(call -> List.of(call.args()))
                .isEqualTo(List.of(7L, MARIBEL));
    }

    @Test
    void theAuthorMarksTheIdeaBoughtWhileNobodyElseGivesIt() {
        jdbc.rows(BY_ID, idea(7, "Gorra", true));

        Idea idea = service.update(CRIS, 7, request("Gorra", true, null));

        assertThat(jdbc.callsWith(UPDATE).get(0).args()).endsWith(true, 7L, CRIS, true);
        assertThat(idea.bought()).isTrue();
    }

    /** R14: once Alba gives it, its author cannot buy it too. */
    @Test
    void theAuthorCannotMarkBoughtAnIdeaSomeoneElseGives() {
        jdbc.rows(BY_ID, claimedBy(idea(7, "Gorra", false), ALBA, "Alba", "RESERVED"));

        RegalosException e = assertRegalosError(() -> service.update(CRIS, 7, request("Gorra", true, null)),
                HttpStatus.CONFLICT, "already_claimed");

        assertThat(e.getMessage()).isEqualTo("Ja la regala Alba");
        assertThat(jdbc.callsWith(UPDATE)).isEmpty();
    }

    @Test
    void someoneItIsSharedWithMaySeeItButNotChangeOrDeleteIt() {
        jdbc.rows(BY_ID, idea(7, "Gorra", false)).updateCount(DELETE, 0);

        assertRegalosError(() -> service.update(ALBA, 7, request("Otra cosa", null, null)), HttpStatus.FORBIDDEN, "forbidden");
        assertRegalosError(() -> service.delete(ALBA, 7), HttpStatus.FORBIDDEN, "forbidden");
        assertThat(jdbc.callsWith(UPDATE)).isEmpty();
    }

    /** To anyone it is not shared with, an idea answers exactly like a missing one. */
    @Test
    void someoneElsesIdeaIsNotFound() {
        jdbc.updateCount(DELETE, 0);

        assertRegalosError(() -> service.update(MARIBEL, 7, request("Gorra", null, null)), HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.delete(MARIBEL, 7), HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.claim(MARIBEL, 7, new IdeaClaimRequest(ClaimState.RESERVED)),
                HttpStatus.NOT_FOUND, "not_found");
        assertThat(jdbc.callsWith(DELETE).get(0).args()).containsExactly(7L, MARIBEL);
        assertThat(jdbc.callsWith(INSERT_CLAIM)).isEmpty();
    }

    @Test
    void someoneItIsSharedWithGivesIt() {
        jdbc.rows(BY_ID, idea(7, "Gorra", false));

        service.claim(ALBA, 7, new IdeaClaimRequest(ClaimState.BOUGHT));

        assertThat(jdbc.callsWith(INSERT_CLAIM).get(0).args()).containsExactly(7L, ALBA, "BOUGHT", 7L, ALBA, 7L);
    }

    @Test
    void whoGivesItChangesTheirOwnClaim() {
        jdbc.rows(BY_ID, claimedBy(idea(7, "Gorra", false), ALBA, "Alba", "RESERVED"));

        service.claim(ALBA, 7, new IdeaClaimRequest(ClaimState.BOUGHT));

        assertThat(jdbc.callsWith(UPDATE_CLAIM).get(0).args()).containsExactly("BOUGHT", 7L, ALBA);
        assertThat(jdbc.callsWith(INSERT_CLAIM)).isEmpty();
    }

    /** R14: one giver per idea, and none once its author bought it. */
    @Test
    void anIdeaSomeoneElseGivesOrItsAuthorBoughtCannotBeClaimed() {
        jdbc.rows(BY_ID, claimedBy(idea(7, "Gorra", false), ALBA, "Alba", "RESERVED"));
        RegalosException taken = assertRegalosError(() -> service.claim(DAVID, 7, new IdeaClaimRequest(ClaimState.RESERVED)),
                HttpStatus.CONFLICT, "already_claimed");
        assertThat(taken.getMessage()).isEqualTo("Ja la regala Alba");

        jdbc.rows(BY_ID, idea(7, "Gorra", true));
        RegalosException bought = assertRegalosError(() -> service.claim(DAVID, 7, new IdeaClaimRequest(ClaimState.RESERVED)),
                HttpStatus.CONFLICT, "already_claimed");
        assertThat(bought.getMessage()).isEqualTo("Ja l'ha comprada Cris");
        assertThat(jdbc.callsWith(INSERT_CLAIM)).isEmpty();
    }

    @Test
    void losingTheRaceForAnIdeaIsAConflict() {
        jdbc.rows(BY_ID, idea(7, "Gorra", false)).updateCount(INSERT_CLAIM, 0);

        assertRegalosError(() -> service.claim(DAVID, 7, new IdeaClaimRequest(ClaimState.RESERVED)),
                HttpStatus.CONFLICT, "already_claimed");
    }

    @Test
    void theAuthorBuysTheirIdeaInsteadOfClaimingIt() {
        jdbc.rows(BY_ID, idea(7, "Gorra", false));

        assertRegalosError(() -> service.claim(CRIS, 7, new IdeaClaimRequest(ClaimState.RESERVED)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.claim(ALBA, 7, null), HttpStatus.BAD_REQUEST, "bad_request");
    }

    @Test
    void onlyWhoClaimedItRemovesTheClaim() {
        jdbc.rows(BY_ID, claimedBy(idea(7, "Gorra", false), ALBA, "Alba", "RESERVED"));

        service.unclaim(ALBA, 7);
        assertThat(jdbc.callsWith(DELETE_CLAIM).get(0).args()).containsExactly(7L, ALBA);

        jdbc.updateCount(DELETE_CLAIM, 0);
        assertRegalosError(() -> service.unclaim(DAVID, 7), HttpStatus.NOT_FOUND, "not_found");
    }
}
