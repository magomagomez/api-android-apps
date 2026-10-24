package com.magomez.androidapps.regalos.auth;

import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.people.Birthday;
import com.magomez.androidapps.regalos.people.Me;
import com.magomez.androidapps.regalos.people.RegalosUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Registration with the invite code, login with the regalos_users accounts, and sessions. */
class RegalosAuthServiceTest {

    private static final RegalosPasswordHasher HASHER = new RegalosPasswordHasher();
    private static final String INVITE = "REGALOS-7Q4K";
    private static final String PASSWORD = "lacre-rojo";
    private static final String STORED = HASHER.hash(PASSWORD);
    private static final String IP = "81.0.0.7";
    private static final String INSERT_USER = "INSERT INTO regalos_users";
    private static final String CREDENTIALS = "SELECT id, password_hash FROM regalos_users WHERE username = ?";
    private static final String BY_ID = "FROM regalos_users WHERE id = ?";
    private static final String INSERT_TOKEN = "INSERT INTO regalos_tokens";
    private static final String TOUCH = "UPDATE regalos_tokens SET last_used_at = now()";
    private static final int CRIS = 2;

    private RegalosFakeJdbc jdbc;
    private RegalosAuthService service;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        service = service(INVITE);
    }

    private RegalosAuthService service(String invite) {
        Clock clock = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC);
        return new RegalosAuthService(new RegalosUserDao(jdbc), new RegalosTokenDao(jdbc), HASHER,
                new LoginThrottle(clock), invite);
    }

    private static RegisterRequest register(String invite, String username) {
        return new RegisterRequest(invite, " Cris ", username, PASSWORD, new Birthday(12, 11));
    }

    @Test
    void registerCreatesTheUserInLowerCaseWithAHashedPasswordAndLogsIn() {
        jdbc.scalar(INSERT_USER, CRIS);

        AuthResponse response = service.register(register(INVITE, "Cris"), IP);

        Object[] args = jdbc.callsWith(INSERT_USER).get(0).args();
        assertThat(args[0]).isEqualTo("cris");
        assertThat(args[1]).isEqualTo("Cris");
        assertThat((String) args[2]).startsWith("pbkdf2$").doesNotContain(PASSWORD);
        assertThat(HASHER.matches(PASSWORD, (String) args[2])).isTrue();
        assertThat(args[3]).isEqualTo(12);
        assertThat(args[4]).isEqualTo(11);
        assertThat(response.user()).isEqualTo(new Me(CRIS, "cris", "Cris", new Birthday(12, 11)));
        assertThat(jdbc.callsWith(INSERT_TOKEN).get(0).args())
                .containsExactly(RegalosSha256.hex(response.token()), CRIS);
    }

    @Test
    void registerWithoutBirthdayIsFine() {
        jdbc.scalar(INSERT_USER, CRIS);

        service.register(new RegisterRequest(INVITE, "Cris", "cris", PASSWORD, null), IP);

        Object[] args = jdbc.callsWith(INSERT_USER).get(0).args();
        assertThat(args[3]).isNull();
        assertThat(args[4]).isNull();
    }

    @Test
    void aWrongInviteIsRefusedBeforeLookingAtAnythingElse() {
        assertRegalosError(() -> service.register(register("REGALOS-0000", "cris"), IP),
                HttpStatus.FORBIDDEN, "invalid_invite");
        assertRegalosError(() -> service.register(register(null, "x"), IP), HttpStatus.FORBIDDEN, "invalid_invite");

        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void withoutAConfiguredInviteNobodyCanRegister() {
        service = service("");

        assertRegalosError(() -> service.register(register("", "cris"), IP), HttpStatus.FORBIDDEN, "invalid_invite");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void tenWrongInvitesFromOneIpLockIt() {
        for (int i = 0; i < 10; i++) {
            assertRegalosError(() -> service.register(register("NOPE", "cris"), IP),
                    HttpStatus.FORBIDDEN, "invalid_invite");
        }

        assertRegalosError(() -> service.register(register(INVITE, "cris"), IP),
                HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts");
    }

    @Test
    void registerValidatesUsernamePasswordNameAndBirthday() {
        String[] badUsernames = {"cr", "x".repeat(41), "cris marin", "cris!", ""};
        for (String username : badUsernames) {
            assertRegalosError(() -> service.register(register(INVITE, username), IP),
                    HttpStatus.BAD_REQUEST, "bad_request");
        }
        assertRegalosError(() -> service.register(new RegisterRequest(INVITE, "Cris", "cris", "corta", null), IP),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.register(new RegisterRequest(INVITE, " ", "cris", PASSWORD, null), IP),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> service.register(
                        new RegisterRequest(INVITE, "Cris", "cris", PASSWORD, new Birthday(30, 2)), IP),
                HttpStatus.BAD_REQUEST, "bad_request");

        assertThat(jdbc.callsWith(INSERT_USER)).isEmpty();
    }

    @Test
    void aTakenUsernameIsAConflict() {
        jdbc.fail(INSERT_USER, new DuplicateKeyException("regalos_users_username_key"));

        assertRegalosError(() -> service.register(register(INVITE, "cris"), IP), HttpStatus.CONFLICT, "username_taken");
        assertThat(jdbc.callsWith(INSERT_TOKEN)).isEmpty();
    }

    @Test
    void loginIgnoresTheCaseOfTheUsername() {
        jdbc.rows(CREDENTIALS, row("id", CRIS, "password_hash", STORED))
                .rows(BY_ID, row("id", CRIS, "username", "cris", "display_name", "Cris",
                        "birthday_day", null, "birthday_month", null));

        AuthResponse response = service.login(new LoginRequest(" CRIS ", PASSWORD), IP);

        assertThat(jdbc.callsWith(CREDENTIALS).get(0).args()).containsExactly("cris");
        assertThat(response.user().displayName()).isEqualTo("Cris");
        assertThat(jdbc.callsWith(INSERT_TOKEN)).hasSize(1);
    }

    @Test
    void aWrongPasswordAndAnUnknownUserGetTheSame401() {
        jdbc.rows(CREDENTIALS, row("id", CRIS, "password_hash", STORED));
        assertRegalosError(() -> service.login(new LoginRequest("cris", "otra-cosa"), IP),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        jdbc.rows(CREDENTIALS);
        jdbc = new RegalosFakeJdbc();
        service = service(INVITE);
        assertRegalosError(() -> service.login(new LoginRequest("nadie", PASSWORD), IP),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");
        assertRegalosError(() -> service.login(new LoginRequest(null, null), IP),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");
        assertRegalosError(() -> service.login(null, IP), HttpStatus.UNAUTHORIZED, "invalid_credentials");

        assertThat(jdbc.callsWith(INSERT_TOKEN)).isEmpty();
    }

    @Test
    void tenWrongPasswordsLockTheUsernameEvenFromAnotherIp() {
        jdbc.rows(CREDENTIALS, row("id", CRIS, "password_hash", STORED));
        for (int i = 0; i < 10; i++) {
            String ip = "10.0.0." + i;
            assertRegalosError(() -> service.login(new LoginRequest("cris", "mal"), ip),
                    HttpStatus.UNAUTHORIZED, "invalid_credentials");
        }

        assertRegalosError(() -> service.login(new LoginRequest("Cris", PASSWORD), "10.9.9.9"),
                HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts");
    }

    @Test
    void authenticateFindsTheSessionByTheHashOfTheToken() {
        jdbc.rows(TOUCH, row("id", CRIS, "display_name", "Cris", "token_hash", RegalosSha256.hex("t")));

        assertThat(service.authenticate("t")).contains(new RegalosCurrentUser(CRIS, "Cris", RegalosSha256.hex("t")));
        assertThat(jdbc.callsWith(TOUCH).get(0).args()).containsExactly(RegalosSha256.hex("t"));
    }

    @Test
    void logoutDeletesTheSession() {
        service.logout(new RegalosCurrentUser(CRIS, "Cris", "hash"));

        assertThat(jdbc.callsWith("DELETE FROM regalos_tokens WHERE token_hash = ?").get(0).args())
                .containsExactly("hash");
    }
}
