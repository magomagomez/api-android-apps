package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.auth.AuthService;
import com.magomez.androidapps.escapersthings.v2.auth.BearerTokenFilter;
import com.magomez.androidapps.escapersthings.v2.auth.CurrentUser;
import com.magomez.androidapps.escapersthings.v2.auth.PasswordHasher;
import com.magomez.androidapps.escapersthings.v2.auth.TokenDao;
import com.magomez.androidapps.escapersthings.v2.auth.UserDao;
import com.magomez.androidapps.escapersthings.v2.config.Sha256;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static com.magomez.androidapps.escapersthings.v2.Rows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Every v2 route needs a live Bearer token, except the login. */
class BearerTokenFilterTest {

    private static final String VALID = "token-valido";
    private static final String ROOMS = "/escapers-api/v2/escape-rooms";
    private static final String TOUCH = "UPDATE escapers_tokens SET last_used_at = now()";
    private static PasswordHasher hasher;

    private RecordingJdbcTemplate jdbc;

    @BeforeAll
    static void hasher() {
        hasher = new PasswordHasher();
    }

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
    }

    @Test
    void withoutTokenIsUnauthorizedWithAJsonError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(new MockHttpServletRequest("GET", ROOMS), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"error\":\"unauthorized\"");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void inventedOrRevokedTokenIsUnauthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(withToken("GET", ROOMS, "inventado"), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(jdbc.last().args()).containsExactly(Sha256.hex("inventado"));
    }

    @Test
    void otherSchemesAreUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ROOMS);
        request.addHeader("Authorization", "Basic " + VALID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        run(request, response);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void liveTokenPassesWithTheUserAsAttribute() throws Exception {
        jdbc.rows(TOUCH, row("id", 3, "name", "Alex", "token_hash", Sha256.hex(VALID)));
        MockHttpServletRequest request = withToken("GET", ROOMS, VALID);

        MockFilterChain chain = run(request, new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(CurrentUser.ATTRIBUTE)).isEqualTo(new CurrentUser(3, "Alex", Sha256.hex(VALID)));
    }

    @Test
    void schemeIsCaseInsensitive() throws Exception {
        jdbc.rows(TOUCH, row("id", 3, "name", "Alex", "token_hash", Sha256.hex(VALID)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/escapers-api/v2/me");
        request.addHeader("Authorization", "bearer " + VALID);

        assertThat(run(request, new MockHttpServletResponse()).getRequest()).isNotNull();
    }

    @Test
    void loginNeedsNoToken() throws Exception {
        MockFilterChain chain = run(new MockHttpServletRequest("POST", "/escapers-api/v2/auth/login"),
                new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void logoutDoesNeedIt() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        run(new MockHttpServletRequest("POST", "/escapers-api/v2/auth/logout"), response);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void v1AndOtherApisAreNotAffected() throws Exception {
        assertThat(run(new MockHttpServletRequest("GET", "/escapers-api/v1/escapeRooms"),
                new MockHttpServletResponse()).getRequest()).isNotNull();
        assertThat(run(new MockHttpServletRequest("GET", "/restaurant-api/v1/restaurants"),
                new MockHttpServletResponse()).getRequest()).isNotNull();
    }

    @Test
    void databaseFailureIsAServerErrorWithoutDetails() throws Exception {
        jdbc.fail(TOUCH, new DataAccessResourceFailureException("connection refused to db.internal"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = run(withToken("GET", ROOMS, VALID), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("server_error").doesNotContain("db.internal");
    }

    private MockFilterChain run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AuthService service = new AuthService(new UserDao(jdbc), new TokenDao(jdbc), hasher);
        MockFilterChain chain = new MockFilterChain();
        new BearerTokenFilter(service).doFilter(request, response, chain);
        return chain;
    }

    private static MockHttpServletRequest withToken(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
