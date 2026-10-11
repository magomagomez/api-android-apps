package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.auth.AuthService;
import com.magomez.androidapps.escapersthings.v2.auth.ChangePasswordRequest;
import com.magomez.androidapps.escapersthings.v2.auth.CurrentUser;
import com.magomez.androidapps.escapersthings.v2.auth.LoginRequest;
import com.magomez.androidapps.escapersthings.v2.auth.LoginResponse;
import com.magomez.androidapps.escapersthings.v2.auth.PasswordHasher;
import com.magomez.androidapps.escapersthings.v2.auth.TokenDao;
import com.magomez.androidapps.escapersthings.v2.auth.UserDao;
import com.magomez.androidapps.escapersthings.v2.config.ApiException;
import com.magomez.androidapps.escapersthings.v2.config.Sha256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.magomez.androidapps.escapersthings.v2.ApiAssertions.assertApiError;
import static com.magomez.androidapps.escapersthings.v2.Rows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Login, sessions and password change of escapers-api v2. */
class AuthServiceTest {

    private static final PasswordHasher HASHER = new PasswordHasher();
    private static final String PASSWORD = "chinchetas";
    private static final String STORED = HASHER.hash(PASSWORD);
    private static final String SESSION = "a".repeat(64);
    private static final CurrentUser CRIS = new CurrentUser(2, "Cris", SESSION);

    private RecordingJdbcTemplate jdbc;
    private AuthService service;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
        service = new AuthService(new UserDao(jdbc), new TokenDao(jdbc), HASHER);
    }

    @Test
    void rightPasswordGivesARandomTokenAndOnlyItsHashIsStored() {
        jdbc.rows("FROM users WHERE name = ?", row("id", 2, "name", "Cris", "password_hash", STORED));

        LoginResponse response = service.login(new LoginRequest("Cris", PASSWORD));

        assertThat(response.user().id()).isEqualTo(2);
        assertThat(response.user().name()).isEqualTo("Cris");
        assertThat(response.token()).matches("[A-Za-z0-9_-]{43}");
        RecordingJdbcTemplate.Call insert = jdbc.last();
        assertThat(insert.sql()).isEqualTo("INSERT INTO escapers_tokens (token_hash, user_id) VALUES (?, ?)");
        assertThat(insert.args()).containsExactly(Sha256.hex(response.token()), 2);
    }

    @Test
    void twoLoginsGetDifferentTokens() {
        jdbc.rows("FROM users WHERE name = ?", row("id", 2, "name", "Cris", "password_hash", STORED));

        assertThat(service.login(new LoginRequest("Cris", PASSWORD)).token())
                .isNotEqualTo(service.login(new LoginRequest("Cris", PASSWORD)).token());
    }

    @Test
    void usernameTravelsAsAParameter() {
        String injection = "x' OR '1'='1";

        assertApiError(() -> service.login(new LoginRequest(injection, PASSWORD)), HttpStatus.UNAUTHORIZED,
                "invalid_credentials");

        assertThat(jdbc.calls.get(0).sql()).doesNotContain(injection);
        assertThat(jdbc.calls.get(0).args()).containsExactly(injection);
    }

    @Test
    void wrongPasswordAndUnknownUserFailTheSameWay() {
        jdbc.rows("FROM users WHERE name = ?", row("id", 2, "name", "Cris", "password_hash", STORED));
        ApiException wrongPassword = assertApiError(() -> service.login(new LoginRequest("Cris", "otra-cosa")),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        RecordingJdbcTemplate empty = new RecordingJdbcTemplate();
        AuthService noUsers = new AuthService(new UserDao(empty), new TokenDao(empty), HASHER);
        ApiException unknownUser = assertApiError(() -> noUsers.login(new LoginRequest("Jos", PASSWORD)),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        assertThat(unknownUser.getMessage()).isEqualTo(wrongPassword.getMessage());
        assertThat(jdbc.callsWith("escapers_tokens")).isEmpty();
    }

    @Test
    void userWithoutHashYetCannotLogIn() {
        jdbc.rows("FROM users WHERE name = ?", row("id", 2, "name", "Cris", "password_hash", null));

        assertApiError(() -> service.login(new LoginRequest("Cris", PASSWORD)), HttpStatus.UNAUTHORIZED,
                "invalid_credentials");
    }

    @Test
    void missingFieldsAreInvalidCredentials() {
        assertApiError(() -> service.login(null), HttpStatus.UNAUTHORIZED, "invalid_credentials");
        assertApiError(() -> service.login(new LoginRequest("Cris", null)), HttpStatus.UNAUTHORIZED,
                "invalid_credentials");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void authenticateLooksUpTheHashOfTheTokenAndTouchesIt() {
        jdbc.rows("UPDATE escapers_tokens SET last_used_at = now()", row("id", 2, "name", "Cris", "token_hash", SESSION));

        assertThat(service.authenticate("raw-token")).contains(CRIS);
        assertThat(jdbc.last().args()).containsExactly(Sha256.hex("raw-token"));
    }

    @Test
    void unknownTokenAuthenticatesNobody() {
        assertThat(service.authenticate("invented")).isEmpty();
    }

    @Test
    void logoutRevokesOnlyTheSessionOfTheRequest() {
        service.logout(CRIS);

        assertThat(jdbc.last().sql()).isEqualTo("DELETE FROM escapers_tokens WHERE token_hash = ?");
        assertThat(jdbc.last().args()).containsExactly(SESSION);
    }

    @Test
    void changingPasswordNeedsTheCurrentOne() {
        jdbc.rows("FROM users WHERE id = ?", row("id", 2, "name", "Cris", "password_hash", STORED));

        assertApiError(() -> service.changePassword(CRIS, new ChangePasswordRequest("mal", "nueva-larga")),
                HttpStatus.FORBIDDEN, "wrong_password");
        assertThat(jdbc.callsWith("UPDATE users")).isEmpty();
        assertThat(jdbc.callsWith("DELETE")).isEmpty();
    }

    @Test
    void newPasswordNeedsEightCharacters() {
        assertApiError(() -> service.changePassword(CRIS, new ChangePasswordRequest(PASSWORD, "1234567")),
                HttpStatus.BAD_REQUEST, "weak_password");
        assertApiError(() -> service.changePassword(CRIS, null), HttpStatus.BAD_REQUEST, "weak_password");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void changedPasswordIsHashedAndTheOtherSessionsAreClosed() {
        jdbc.rows("FROM users WHERE id = ?", row("id", 2, "name", "Cris", "password_hash", STORED));

        service.changePassword(CRIS, new ChangePasswordRequest(PASSWORD, "12345678"));

        RecordingJdbcTemplate.Call update = jdbc.callsWith("UPDATE users SET password_hash = ? WHERE id = ?").get(0);
        assertThat(HASHER.matches("12345678", (String) update.args()[0])).isTrue();
        assertThat(update.args()[1]).isEqualTo(2);
        assertThat(jdbc.last().sql()).isEqualTo("DELETE FROM escapers_tokens WHERE user_id = ? AND token_hash <> ?");
        assertThat(jdbc.last().args()).containsExactly(2, SESSION);
    }

    @Test
    void usersAreListedById() {
        jdbc.rows("SELECT id, name FROM users ORDER BY id", row("id", 1, "name", "Magomez"), row("id", 2, "name", "Cris"));

        assertThat(service.users()).extracting(u -> u.name()).containsExactly("Magomez", "Cris");
    }
}
