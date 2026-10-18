package com.magomez.androidapps.jcfit.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Every jcfit-api route needs a live Bearer token, except the login; nothing else is touched. */
class JcfitBearerTokenFilterTest {

    private static final JcfitPasswordHasher HASHER = new JcfitPasswordHasher();
    private static final String VALID = "token-valido";
    private static final String WEEK = "/jcfit-api/v1/weeks/2026-10-15";
    private static final String TOUCH = "UPDATE jcfit_tokens SET last_used_at = now()";
    private static final String UNAUTHORIZED_BODY =
            "{\"error\":\"unauthorized\",\"message\":\"Falta la sesión o ha caducado; vuelve a entrar\"}";

    private JcfitRecordingJdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
    }

    @Test
    void withoutTokenIsUnauthorizedWithTheContractBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(new MockHttpServletRequest("GET", WEEK), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).isEqualTo(UNAUTHORIZED_BODY);
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void inventedOrRevokedTokenIsUnauthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(withToken("GET", WEEK, "inventado"), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEqualTo(UNAUTHORIZED_BODY);
        assertThat(jdbc.last().args()).containsExactly(JcfitSha256.hex("inventado"));
    }

    @Test
    void otherSchemesAreUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", WEEK);
        request.addHeader("Authorization", "Basic " + VALID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        run(request, response);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void liveTokenPassesWithTheUserAsAttribute() throws Exception {
        jdbc.rows(TOUCH, row("id", 1, "username", "magomez", "token_hash", JcfitSha256.hex(VALID)));
        MockHttpServletRequest request = withToken("GET", "/jcfit-api/v1/admin/exercises", VALID);

        MockFilterChain chain = run(request, new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(JcfitCurrentUser.ATTRIBUTE))
                .isEqualTo(new JcfitCurrentUser(1, "magomez", JcfitSha256.hex(VALID)));
    }

    @Test
    void loginNeedsNoToken() throws Exception {
        MockFilterChain chain = run(new MockHttpServletRequest("POST", "/jcfit-api/v1/auth/login"),
                new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void logoutAndOtherMethodsOnLoginDoNeedIt() throws Exception {
        MockHttpServletResponse logout = new MockHttpServletResponse();
        run(new MockHttpServletRequest("POST", "/jcfit-api/v1/auth/logout"), logout);
        MockHttpServletResponse getLogin = new MockHttpServletResponse();
        run(new MockHttpServletRequest("GET", "/jcfit-api/v1/auth/login"), getLogin);

        assertThat(logout.getStatus()).isEqualTo(401);
        assertThat(getLogin.getStatus()).isEqualTo(401);
    }

    @Test
    void routesOutsideJcfitApiAreNotFiltered() throws Exception {
        for (String uri : new String[]{"/escapers-api/v2/escape-rooms", "/jcfit-admin/index.html", "/jcfit-admin/",
                "/jcfit-api/v1", "/jcfit-api/v10/weeks"}) {
            MockFilterChain chain = run(new MockHttpServletRequest("GET", uri), new MockHttpServletResponse());
            assertThat(chain.getRequest()).as(uri).isNotNull();
        }
        assertThat(jdbc.calls).isEmpty();
    }

    /** Spring routes these to jcfit controllers, so the filter must see them as jcfit routes too. */
    @Test
    void disguisedJcfitRoutesAreStillFiltered() throws Exception {
        for (String uri : new String[]{"/jcfit-api/v1;x=1/admin/exercises", "/jcfit-api//v1/admin/exercises",
                "/jcfit-api/%761/admin/exercises", "/jcfit-api/v1/auth/login;x=1/../../admin/program"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = run(new MockHttpServletRequest("GET", uri), response);
            assertThat(chain.getRequest()).as(uri).isNull();
            assertThat(response.getStatus()).as(uri).isEqualTo(401);
        }
    }

    @Test
    void databaseFailureIsAServerErrorWithoutDetails() throws Exception {
        jdbc.fail(TOUCH, new DataAccessResourceFailureException("connection refused to db.internal"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = run(withToken("GET", WEEK, VALID), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
                .isEqualTo("{\"error\":\"server_error\",\"message\":\"Error interno, inténtalo más tarde\"}");
    }

    private MockFilterChain run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        JcfitAuthService service = new JcfitAuthService(new JcfitUserDao(jdbc), new JcfitTokenDao(jdbc), HASHER);
        MockFilterChain chain = new MockFilterChain();
        new JcfitBearerTokenFilter(service).doFilter(request, response, chain);
        return chain;
    }

    private static MockHttpServletRequest withToken(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
