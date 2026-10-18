package com.magomez.androidapps.jcfit.auth;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.assertApiError;
import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Login and logout of jcfit-api, with the accounts of the shared users table. */
class JcfitAuthServiceTest {

    private static final JcfitPasswordHasher HASHER = new JcfitPasswordHasher();
    private static final String PASSWORD = "sentadillas";
    private static final String STORED = HASHER.hash(PASSWORD);
    private static final String BY_NAME = "FROM users WHERE name = ?";
    private static final String SESSION = "a".repeat(64);
    private static final int OWNER = 1;
    private static final int OTHER_ESCAPER = 2;
    private static final int NOBODY = 0;

    private JcfitRecordingJdbcTemplate jdbc;
    private JcfitAuthService service;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        service = service(jdbc, OWNER);
    }

    @Test
    void rightPasswordGivesARandomTokenAndOnlyItsHashIsStored() {
        jdbc.rows(BY_NAME, row("id", OWNER, "name", "Magomez", "password_hash", STORED));

        JcfitLoginResponse response = service.login(new JcfitLoginRequest("Magomez", PASSWORD));

        assertThat(response.token()).matches("[A-Za-z0-9_-]{43}");
        JcfitRecordingJdbcTemplate.Call insert = jdbc.last();
        assertThat(insert.sql()).isEqualTo("INSERT INTO jcfit_tokens (token_hash, user_id) VALUES (?, ?)");
        assertThat(insert.args()).containsExactly(JcfitSha256.hex(response.token()), OWNER);
    }

    @Test
    void twoLoginsGetDifferentTokens() {
        jdbc.rows(BY_NAME, row("id", OWNER, "name", "Magomez", "password_hash", STORED));

        assertThat(service.login(new JcfitLoginRequest("Magomez", PASSWORD)).token())
                .isNotEqualTo(service.login(new JcfitLoginRequest("Magomez", PASSWORD)).token());
    }

    @Test
    void anotherEscapersUserCannotLogInEvenWithTheRightPassword() {
        jdbc.rows(BY_NAME, row("id", OTHER_ESCAPER, "name", "Cris", "password_hash", STORED));

        ApiException e = assertApiError(() -> service.login(new JcfitLoginRequest("Cris", PASSWORD)),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        assertThat(e.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
        assertThat(jdbc.callsWith("jcfit_tokens")).isEmpty();
    }

    @Test
    void withoutAnAllowedUserNobodyLogsIn() {
        jdbc.rows(BY_NAME, row("id", OWNER, "name", "Magomez", "password_hash", STORED));

        assertApiError(() -> service(jdbc, NOBODY).login(new JcfitLoginRequest("Magomez", PASSWORD)),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");
        assertThat(jdbc.callsWith("jcfit_tokens")).isEmpty();
    }

    @Test
    void anAccountWithoutPasswordHashCannotLogIn() {
        jdbc.rows(BY_NAME, row("id", OWNER, "name", "Magomez", "password_hash", null));

        assertApiError(() -> service.login(new JcfitLoginRequest("Magomez", PASSWORD)), HttpStatus.UNAUTHORIZED,
                "invalid_credentials");
    }

    @Test
    void wrongPasswordAndUnknownUserFailTheSameWay() {
        jdbc.rows(BY_NAME, row("id", OWNER, "name", "Magomez", "password_hash", STORED));
        ApiException wrongPassword = assertApiError(() -> service.login(new JcfitLoginRequest("Magomez", "otra")),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        JcfitRecordingJdbcTemplate empty = new JcfitRecordingJdbcTemplate();
        ApiException unknownUser = assertApiError(
                () -> service(empty, OWNER).login(new JcfitLoginRequest("nadie", PASSWORD)),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        assertThat(wrongPassword.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
        assertThat(unknownUser.getMessage()).isEqualTo(wrongPassword.getMessage());
        assertThat(jdbc.callsWith("jcfit_tokens")).isEmpty();
        assertThat(empty.callsWith("jcfit_tokens")).isEmpty();
    }

    @Test
    void usernameTravelsAsAParameter() {
        String injection = "x' OR '1'='1";

        assertApiError(() -> service.login(new JcfitLoginRequest(injection, PASSWORD)), HttpStatus.UNAUTHORIZED,
                "invalid_credentials");

        assertThat(jdbc.calls.get(0).sql()).doesNotContain(injection);
        assertThat(jdbc.calls.get(0).args()).containsExactly(injection);
    }

    @Test
    void missingFieldsAreInvalidCredentials() {
        assertApiError(() -> service.login(null), HttpStatus.UNAUTHORIZED, "invalid_credentials");
        assertApiError(() -> service.login(new JcfitLoginRequest(null, PASSWORD)), HttpStatus.UNAUTHORIZED,
                "invalid_credentials");
        assertApiError(() -> service.login(new JcfitLoginRequest("Magomez", null)), HttpStatus.UNAUTHORIZED,
                "invalid_credentials");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void authenticateLooksUpTheHashOfTheTokenAndTouchesIt() {
        jdbc.rows("UPDATE jcfit_tokens SET last_used_at = now()",
                row("id", OWNER, "name", "Magomez", "token_hash", SESSION));

        assertThat(service.authenticate("raw-token")).contains(new JcfitCurrentUser(OWNER, "Magomez", SESSION));
        assertThat(jdbc.last().args()).containsExactly(JcfitSha256.hex("raw-token"));
    }

    @Test
    void aSessionOfAnAccountThatIsNoLongerAllowedAuthenticatesNobody() {
        jdbc.rows("UPDATE jcfit_tokens SET last_used_at = now()",
                row("id", OTHER_ESCAPER, "name", "Cris", "token_hash", SESSION));

        assertThat(service.authenticate("raw-token")).isEmpty();
    }

    @Test
    void unknownTokenAuthenticatesNobody() {
        assertThat(service.authenticate("inventado")).isEmpty();
    }

    @Test
    void logoutRevokesOnlyTheSessionOfTheRequest() {
        service.logout(new JcfitCurrentUser(OWNER, "Magomez", SESSION));

        assertThat(jdbc.last().sql()).isEqualTo("DELETE FROM jcfit_tokens WHERE token_hash = ?");
        assertThat(jdbc.last().args()).containsExactly(SESSION);
    }

    private static JcfitAuthService service(JcfitRecordingJdbcTemplate jdbc, int allowedUserId) {
        return new JcfitAuthService(new JcfitUserDao(jdbc), new JcfitTokenDao(jdbc), HASHER, allowedUserId);
    }
}
