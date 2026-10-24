package com.magomez.androidapps.regalos.people;

import com.magomez.androidapps.regalos.config.RegalosConfig;
import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
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

/** My profile and the other people of the app, nearest birthday first. */
class PeopleServiceTest {

    /** Still the 23rd in UTC, but already 00:30 on the 24th of October in Madrid. */
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T22:30:00Z"), RegalosConfig.SPAIN);
    private static final String OTHERS = "FROM regalos_users u LEFT JOIN regalos_items i";
    private static final String BY_ID = "FROM regalos_users WHERE id = ?";
    private static final String UPDATE = "UPDATE regalos_users SET display_name = ?";
    private static final int MAGOMEZ = 1;

    private RegalosFakeJdbc jdbc;
    private PeopleService service;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        service = new PeopleService(new RegalosUserDao(jdbc), CLOCK);
    }

    static Map<String, Object> user(int id, String username, String name, Integer day, Integer month) {
        return row("id", id, "username", username, "display_name", name, "birthday_day", day, "birthday_month", month);
    }

    private static Map<String, Object> others(int id, String name, Integer day, Integer month,
                                              long wishes, long free, long mine) {
        return row("id", id, "display_name", name, "birthday_day", day, "birthday_month", month,
                "wish_count", wishes, "free_count", free, "mine_count", mine);
    }

    @Test
    void peopleLeaveMeOutAndCountTheWishesIGive() {
        jdbc.rows(OTHERS, others(2, "Cris", 12, 11, 8, 3, 1));

        List<PersonSummary> people = service.people(MAGOMEZ);

        assertThat(people).containsExactly(
                new PersonSummary(2, "Cris", new Birthday(12, 11), 19, 8, 3, 1));
        assertThat(jdbc.callsWith(OTHERS).get(0).args()).containsExactly(MAGOMEZ, MAGOMEZ);
        assertThat(jdbc.callsWith(OTHERS).get(0).sql())
                .contains("i.status = 'ACTIVE'")
                .contains("WHERE u.id <> ?");
    }

    @Test
    void peopleGoNearestBirthdayFirstThenThoseWithoutOneByName() {
        jdbc.rows(OTHERS,
                others(5, "Uri", null, null, 0, 0, 0),
                others(3, "Alex", 3, 2, 5, 5, 0),
                others(4, "Sara", 27, 6, 11, 4, 2),
                others(2, "Cris", 12, 11, 8, 3, 1),
                others(6, "Bea", null, null, 1, 1, 0),
                others(7, "Ana", 24, 10, 2, 2, 0));

        List<String> order = service.people(MAGOMEZ).stream().map(PersonSummary::displayName).toList();

        assertThat(order).containsExactly("Ana", "Cris", "Alex", "Sara", "Bea", "Uri");
    }

    @Test
    void meIsMyProfile() {
        jdbc.rows(BY_ID, user(MAGOMEZ, "magomez", "Magomez", null, null));

        assertThat(service.me(MAGOMEZ)).isEqualTo(new Me(MAGOMEZ, "magomez", "Magomez", null));
    }

    @Test
    void updateMeTrimsTheNameAndStoresTheBirthday() {
        jdbc.rows(BY_ID, user(MAGOMEZ, "magomez", "Mago", 5, 3));

        Me me = service.updateMe(MAGOMEZ, new ProfileRequest("  Mago ", new Birthday(5, 3)));

        assertThat(jdbc.callsWith(UPDATE).get(0).args()).containsExactly("Mago", 5, 3, MAGOMEZ);
        assertThat(me.displayName()).isEqualTo("Mago");
    }

    @Test
    void updateMeCanRemoveTheBirthday() {
        jdbc.rows(BY_ID, user(MAGOMEZ, "magomez", "Magomez", null, null));

        service.updateMe(MAGOMEZ, new ProfileRequest("Magomez", null));

        assertThat(jdbc.callsWith(UPDATE).get(0).args()).containsExactly("Magomez", null, null, MAGOMEZ);
    }

    @Test
    void updateMeRejectsABlankOrTooLongNameAndAWrongBirthday() {
        assertRegalosError(() -> service.updateMe(MAGOMEZ, new ProfileRequest("   ", null)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.updateMe(MAGOMEZ, new ProfileRequest("x".repeat(41), null)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.updateMe(MAGOMEZ, new ProfileRequest("Mago", new Birthday(31, 2))),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.updateMe(MAGOMEZ, null), HttpStatus.BAD_REQUEST, "bad_request");

        assertThat(jdbc.callsWith(UPDATE)).isEmpty();
    }

    @Test
    void personIsEmptyForAnUnknownId() {
        assertThat(service.person(99)).isEmpty();
    }

    @Test
    void personCarriesTheDaysUntilItsBirthday() {
        jdbc.rows(BY_ID, user(2, "cris", "Cris", 12, 11));

        assertThat(service.person(2)).contains(new Person(2, "Cris", new Birthday(12, 11), 19));
    }
}
