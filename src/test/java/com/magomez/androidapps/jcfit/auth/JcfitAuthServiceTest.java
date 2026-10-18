package com.magomez.androidapps.jcfit.auth;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.assertApiError;
import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Login and logout of jcfit-api. */
class JcfitAuthServiceTest {

    private static final JcfitPasswordHasher HASHER = new JcfitPasswordHasher();
    private static final String PASSWORD = "sentadillas";
    private static final String STORED = HASHER.hash(PASSWORD);
    private static final String BY_NAME = "FROM jcfit_users WHERE username = ?";
    private static final String SESSION = "a".repeat(64);

    private JcfitRecordingJdbcTemplate jdbc;
    private JcfitAuthService service;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        service = service(jdbc);
    }

    @Test
    void rightPasswordGivesARandomTokenAndOnlyItsHashIsStored() {
        jdbc.rows(BY_NAME, row("id", 1, "username", "magomez", "password_hash", STORED));

        JcfitLoginResponse response = service.login(new JcfitLoginRequest("magomez", PASSWORD));

        assertThat(response.token()).matches("[A-Za-z0-9_-]{43}");
        JcfitRecordingJdbcTemplate.Call insert = jdbc.last();
        assertThat(insert.sql()).isEqualTo("INSERT INTO jcfit_tokens (token_hash, user_id) VALUES (?, ?)");
        assertThat(insert.args()).containsExactly(JcfitSha256.hex(response.token()), 1);
    }

    @Test
    void twoLoginsGetDifferentTokens() {
        jdbc.rows(BY_NAME, row("id", 1, "username", "magomez", "password_hash", STORED));

        assertThat(service.login(new JcfitLoginRequest("magomez", PASSWORD)).token())
                .isNotEqualTo(service.login(new JcfitLoginRequest("magomez", PASSWORD)).token());
    }

    @Test
    void wrongPasswordAndUnknownUserFailTheSameWay() {
        jdbc.rows(BY_NAME, row("id", 1, "username", "magomez", "password_hash", STORED));
        ApiException wrongPassword = assertApiError(() -> service.login(new JcfitLoginRequest("magomez", "otra")),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        JcfitRecordingJdbcTemplate empty = new JcfitRecordingJdbcTemplate();
        ApiException unknownUser = assertApiError(() -> service(empty).login(new JcfitLoginRequest("nadie", PASSWORD)),
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
        assertApiError(() -> service.login(new JcfitLoginRequest("magomez", null)), HttpStatus.UNAUTHORIZED,
                "invalid_credentials");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void authenticateLooksUpTheHashOfTheTokenAndTouchesIt() {
        jdbc.rows("UPDATE jcfit_tokens SET last_used_at = now()",
                row("id", 1, "username", "magomez", "token_hash", SESSION));

        assertThat(service.authenticate("raw-token")).contains(new JcfitCurrentUser(1, "magomez", SESSION));
        assertThat(jdbc.last().args()).containsExactly(JcfitSha256.hex("raw-token"));
    }

    @Test
    void unknownTokenAuthenticatesNobody() {
        assertThat(service.authenticate("inventado")).isEmpty();
    }

    @Test
    void logoutRevokesOnlyTheSessionOfTheRequest() {
        service.logout(new JcfitCurrentUser(1, "magomez", SESSION));

        assertThat(jdbc.last().sql()).isEqualTo("DELETE FROM jcfit_tokens WHERE token_hash = ?");
        assertThat(jdbc.last().args()).containsExactly(SESSION);
    }

    private static JcfitAuthService service(JcfitRecordingJdbcTemplate jdbc) {
        return new JcfitAuthService(new JcfitUserDao(jdbc), new JcfitTokenDao(jdbc), HASHER);
    }
}
