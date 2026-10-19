package com.magomez.androidapps.jcfit.auth;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.assertApiError;
import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Login and logout of jcfit-api, with the accounts of the shared users table and their two roles. */
class JcfitAuthServiceTest {

    private static final JcfitPasswordHasher HASHER = new JcfitPasswordHasher();
    private static final String PASSWORD = "sentadillas";
    private static final String STORED = HASHER.hash(PASSWORD);
    private static final String BY_NAME = "FROM users WHERE name = ?";
    private static final String TOUCH = "UPDATE jcfit_tokens SET last_used_at = now()";
    private static final String SESSION = "a".repeat(64);
    private static final int CRIS = 2;
    private static final int MAGOMEZ = 1;
    private static final int OTHER_ESCAPER = 7;
    private static final int NOBODY = 0;

    private JcfitRecordingJdbcTemplate jdbc;
    private JcfitAuthService service;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        service = service(jdbc, CRIS, MAGOMEZ);
    }

    @Test
    void rightPasswordGivesARandomTokenAndOnlyItsHashIsStored() {
        jdbc.rows(BY_NAME, row("id", CRIS, "name", "Cris", "password_hash", STORED));

        JcfitLoginResponse response = service.login(new JcfitLoginRequest("Cris", PASSWORD), JcfitRole.APP);

        assertThat(response.token()).matches("[A-Za-z0-9_-]{43}");
        JcfitRecordingJdbcTemplate.Call insert = jdbc.last();
        assertThat(insert.sql()).isEqualTo("INSERT INTO jcfit_tokens (token_hash, user_id) VALUES (?, ?)");
        assertThat(insert.args()).containsExactly(JcfitSha256.hex(response.token()), CRIS);
    }

    @Test
    void twoLoginsGetDifferentTokens() {
        jdbc.rows(BY_NAME, row("id", CRIS, "name", "Cris", "password_hash", STORED));

        assertThat(service.login(new JcfitLoginRequest("Cris", PASSWORD), JcfitRole.APP).token())
                .isNotEqualTo(service.login(new JcfitLoginRequest("Cris", PASSWORD), JcfitRole.APP).token());
    }

    @Test
    void anotherEscapersUserCannotLogInAnywhereEvenWithTheRightPassword() {
        jdbc.rows(BY_NAME, row("id", OTHER_ESCAPER, "name", "Pepe", "password_hash", STORED));

        for (JcfitRole role : JcfitRole.values()) {
            ApiException e = assertApiError(() -> service.login(new JcfitLoginRequest("Pepe", PASSWORD), role),
                    HttpStatus.UNAUTHORIZED, "invalid_credentials");
            assertThat(e.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
        }
        assertThat(jdbc.callsWith("jcfit_tokens")).isEmpty();
    }

    @Test
    void theAppLoginRejectsTheBackofficeAccountLikeAWrongPassword() {
        jdbc.rows(BY_NAME, row("id", MAGOMEZ, "name", "Magomez", "password_hash", STORED));

        ApiException e = assertApiError(() -> service.login(new JcfitLoginRequest("Magomez", PASSWORD), JcfitRole.APP),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        assertThat(e.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
        assertThat(jdbc.callsWith("jcfit_tokens")).isEmpty();
    }

    @Test
    void theBackofficeLoginLetsTheBackofficeAccountIn() {
        jdbc.rows(BY_NAME, row("id", MAGOMEZ, "name", "Magomez", "password_hash", STORED));

        JcfitLoginResponse response = service.login(new JcfitLoginRequest("Magomez", PASSWORD), JcfitRole.ADMIN);

        assertThat(jdbc.last().args()).containsExactly(JcfitSha256.hex(response.token()), MAGOMEZ);
    }

    @Test
    void theBackofficeLoginRejectsTheAppAccountLikeAWrongPassword() {
        jdbc.rows(BY_NAME, row("id", CRIS, "name", "Cris", "password_hash", STORED));

        ApiException e = assertApiError(() -> service.login(new JcfitLoginRequest("Cris", PASSWORD), JcfitRole.ADMIN),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        assertThat(e.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
        assertThat(jdbc.callsWith("jcfit_tokens")).isEmpty();
    }

    @Test
    void wrongPasswordOnTheBackofficeFailsTheSameWay() {
        jdbc.rows(BY_NAME, row("id", MAGOMEZ, "name", "Magomez", "password_hash", STORED));

        ApiException e = assertApiError(() -> service.login(new JcfitLoginRequest("Magomez", "otra"), JcfitRole.ADMIN),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        assertThat(e.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
    }

    @Test
    void oneAccountWithBothRolesLogsInToBothSitesAndEntersBoth() {
        JcfitAuthService both = service(jdbc, MAGOMEZ, MAGOMEZ);
        jdbc.rows(BY_NAME, row("id", MAGOMEZ, "name", "Magomez", "password_hash", STORED));

        both.login(new JcfitLoginRequest("Magomez", PASSWORD), JcfitRole.APP);
        both.login(new JcfitLoginRequest("Magomez", PASSWORD), JcfitRole.ADMIN);

        assertThat(jdbc.callsWith("INSERT INTO jcfit_tokens")).hasSize(2);
        JcfitCurrentUser user = new JcfitCurrentUser(MAGOMEZ, "Magomez", SESSION);
        assertThat(both.canEnter(user, JcfitRole.APP)).isTrue();
        assertThat(both.canEnter(user, JcfitRole.ADMIN)).isTrue();
    }

    @Test
    void eachAccountEntersOnlyItsSite() {
        JcfitCurrentUser cris = new JcfitCurrentUser(CRIS, "Cris", SESSION);
        JcfitCurrentUser magomez = new JcfitCurrentUser(MAGOMEZ, "Magomez", SESSION);

        assertThat(service.canEnter(cris, JcfitRole.APP)).isTrue();
        assertThat(service.canEnter(cris, JcfitRole.ADMIN)).isFalse();
        assertThat(service.canEnter(magomez, JcfitRole.ADMIN)).isTrue();
        assertThat(service.canEnter(magomez, JcfitRole.APP)).isFalse();
    }

    @Test
    void withoutConfiguredAccountsNobodyLogsIn() {
        jdbc.rows(BY_NAME, row("id", CRIS, "name", "Cris", "password_hash", STORED));
        JcfitAuthService nobody = service(jdbc, NOBODY, NOBODY);

        for (JcfitRole role : JcfitRole.values()) {
            assertApiError(() -> nobody.login(new JcfitLoginRequest("Cris", PASSWORD), role),
                    HttpStatus.UNAUTHORIZED, "invalid_credentials");
        }
        assertThat(jdbc.callsWith("jcfit_tokens")).isEmpty();
    }

    @Test
    void anAccountWithoutPasswordHashCannotLogIn() {
        jdbc.rows(BY_NAME, row("id", CRIS, "name", "Cris", "password_hash", null));

        assertApiError(() -> service.login(new JcfitLoginRequest("Cris", PASSWORD), JcfitRole.APP),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");
    }

    @Test
    void wrongPasswordAndUnknownUserFailTheSameWay() {
        jdbc.rows(BY_NAME, row("id", CRIS, "name", "Cris", "password_hash", STORED));
        ApiException wrongPassword = assertApiError(
                () -> service.login(new JcfitLoginRequest("Cris", "otra"), JcfitRole.APP),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        JcfitRecordingJdbcTemplate empty = new JcfitRecordingJdbcTemplate();
        ApiException unknownUser = assertApiError(
                () -> service(empty, CRIS, MAGOMEZ).login(new JcfitLoginRequest("nadie", PASSWORD), JcfitRole.APP),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        assertThat(wrongPassword.getMessage()).isEqualTo("Usuario o contraseña incorrectos");
        assertThat(unknownUser.getMessage()).isEqualTo(wrongPassword.getMessage());
        assertThat(jdbc.callsWith("jcfit_tokens")).isEmpty();
        assertThat(empty.callsWith("jcfit_tokens")).isEmpty();
    }

    @Test
    void usernameTravelsAsAParameter() {
        String injection = "x' OR '1'='1";

        assertApiError(() -> service.login(new JcfitLoginRequest(injection, PASSWORD), JcfitRole.APP),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");

        assertThat(jdbc.calls.get(0).sql()).doesNotContain(injection);
        assertThat(jdbc.calls.get(0).args()).containsExactly(injection);
    }

    @Test
    void missingFieldsAreInvalidCredentials() {
        assertApiError(() -> service.login(null, JcfitRole.APP), HttpStatus.UNAUTHORIZED, "invalid_credentials");
        assertApiError(() -> service.login(new JcfitLoginRequest(null, PASSWORD), JcfitRole.ADMIN),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");
        assertApiError(() -> service.login(new JcfitLoginRequest("Cris", null), JcfitRole.APP),
                HttpStatus.UNAUTHORIZED, "invalid_credentials");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void authenticateLooksUpTheHashOfTheTokenAndTouchesIt() {
        jdbc.rows(TOUCH, row("id", CRIS, "name", "Cris", "token_hash", SESSION));

        assertThat(service.authenticate("raw-token")).contains(new JcfitCurrentUser(CRIS, "Cris", SESSION));
        assertThat(jdbc.last().args()).containsExactly(JcfitSha256.hex("raw-token"));
    }

    @Test
    void aSessionOfTheBackofficeAccountAuthenticatesToo() {
        jdbc.rows(TOUCH, row("id", MAGOMEZ, "name", "Magomez", "token_hash", SESSION));

        assertThat(service.authenticate("raw-token")).contains(new JcfitCurrentUser(MAGOMEZ, "Magomez", SESSION));
    }

    @Test
    void aSessionOfAnAccountWithoutAnyRoleLeftAuthenticatesNobody() {
        jdbc.rows(TOUCH, row("id", OTHER_ESCAPER, "name", "Pepe", "token_hash", SESSION));

        assertThat(service.authenticate("raw-token")).isEmpty();
    }

    @Test
    void unknownTokenAuthenticatesNobody() {
        assertThat(service.authenticate("inventado")).isEmpty();
    }

    @Test
    void logoutRevokesOnlyTheSessionOfTheRequest() {
        service.logout(new JcfitCurrentUser(CRIS, "Cris", SESSION));

        assertThat(jdbc.last().sql()).isEqualTo("DELETE FROM jcfit_tokens WHERE token_hash = ?");
        assertThat(jdbc.last().args()).containsExactly(SESSION);
    }

    private static JcfitAuthService service(JcfitRecordingJdbcTemplate jdbc, int appUserId, int adminUserId) {
        return new JcfitAuthService(new JcfitUserDao(jdbc), new JcfitTokenDao(jdbc), HASHER, appUserId, adminUserId);
    }
}
