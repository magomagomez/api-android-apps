package com.magomez.androidapps.regalos.ideas;

import com.magomez.androidapps.regalos.config.RegalosConfig;
import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.people.RegalosUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** My ideas for someone else: only their author ever reads or changes them (R7). */
class IdeaServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), RegalosConfig.SPAIN);
    private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-10-30T10:00:00Z");
    private static final String FOR_PERSON = "FROM regalos_ideas WHERE author_user_id = ? AND for_user_id = ?";
    private static final String BY_ID = "FROM regalos_ideas WHERE author_user_id = ? AND id = ?";
    private static final String INSERT = "INSERT INTO regalos_ideas";
    private static final String UPDATE = "UPDATE regalos_ideas SET title = ?";
    private static final String DELETE = "DELETE FROM regalos_ideas";
    private static final String USER_BY_ID = "FROM regalos_users WHERE id = ?";
    private static final int CRIS = 1;
    private static final int JAVI = 2;

    private RegalosFakeJdbc jdbc;
    private IdeaService service;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        service = new IdeaService(new IdeaDao(jdbc), new PeopleService(new RegalosUserDao(jdbc), CLOCK));
    }

    static Map<String, Object> idea(long id, String title, boolean bought) {
        return row("id", id, "for_user_id", JAVI, "title", title, "url", null, "store", null, "price_cents", 2200,
                "notes", null, "bought", bought, "created_at", CREATED);
    }

    private void javiExists() {
        jdbc.rows(USER_BY_ID, row("id", JAVI, "username", "javi", "display_name", "Javi",
                "birthday_day", null, "birthday_month", null));
    }

    @Test
    void myIdeasForSomeoneAreFilteredByMeAsAuthor() {
        javiExists();
        jdbc.rows(FOR_PERSON, idea(7, "Gorra para correr", false));

        assertThat(service.list(CRIS, JAVI)).extracting(Idea::title).containsExactly("Gorra para correr");
        assertThat(jdbc.callsWith(FOR_PERSON).get(0).args()).containsExactly(CRIS, JAVI);
    }

    @Test
    void ideasForMyselfAreForbidden() {
        assertRegalosError(() -> service.list(CRIS, CRIS), HttpStatus.FORBIDDEN, "forbidden");
        assertRegalosError(() -> service.create(CRIS, CRIS, new IdeaRequest("Algo", null, null, null, null, null)),
                HttpStatus.FORBIDDEN, "forbidden");
        assertThat(jdbc.callsWith("regalos_ideas")).isEmpty();
    }

    @Test
    void ideasForAnUnknownPersonAreNotFound() {
        assertRegalosError(() -> service.list(CRIS, 99), HttpStatus.NOT_FOUND, "not_found");
    }

    @Test
    void createValidatesLikeAWishAndStoresMeAsAuthor() {
        javiExists();
        jdbc.scalar(INSERT, 7L).rows(BY_ID, idea(7, "Gorra para correr", false));

        Idea idea = service.create(CRIS, JAVI,
                new IdeaRequest(" Gorra para correr ", "https://www.decathlon.es/gorra", null, 2200, null, null));

        assertThat(jdbc.callsWith(INSERT).get(0).args())
                .containsExactly(CRIS, JAVI, "Gorra para correr", "https://www.decathlon.es/gorra", "decathlon.es", 2200,
                        null, false);
        assertThat(idea.id()).isEqualTo(7L);
        assertRegalosError(() -> service.create(CRIS, JAVI, new IdeaRequest(" ", null, null, null, null, null)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.create(CRIS, JAVI,
                        new IdeaRequest("Gorra", "javascript:alert(1)", null, null, null, null)),
                HttpStatus.BAD_REQUEST, "bad_request");
    }

    @Test
    void updateAndMarkBoughtOnlyMyOwnIdea() {
        jdbc.rows(BY_ID, idea(7, "Gorra", true));

        Idea idea = service.update(CRIS, 7, new IdeaRequest("Gorra", null, null, 2200, null, true));

        assertThat(jdbc.callsWith(UPDATE).get(0).args()).endsWith(true, 7L, CRIS);
        assertThat(idea.bought()).isTrue();
    }

    /** Someone else's idea answers exactly like a missing one: nothing to learn from the id. */
    @Test
    void someoneElsesIdeaIsNotFound() {
        jdbc.updateCount(UPDATE, 0).updateCount(DELETE, 0);

        assertRegalosError(() -> service.update(CRIS, 7, new IdeaRequest("Gorra", null, null, null, null, null)),
                HttpStatus.NOT_FOUND, "not_found");
        assertRegalosError(() -> service.delete(CRIS, 7), HttpStatus.NOT_FOUND, "not_found");
        assertThat(jdbc.callsWith(DELETE).get(0).args()).containsExactly(7L, CRIS);
    }
}
