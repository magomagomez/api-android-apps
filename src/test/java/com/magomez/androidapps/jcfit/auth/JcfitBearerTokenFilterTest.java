package com.magomez.androidapps.jcfit.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every jcfit-api route needs a live Bearer token, except both logins; /admin is only for the
 * backoffice account and the rest only for the app account. Nothing else is touched.
 */
class JcfitBearerTokenFilterTest {

    private static final JcfitPasswordHasher HASHER = new JcfitPasswordHasher();
    private static final int CRIS = 2;
    private static final int MAGOMEZ = 1;
    private static final int OTHER_ESCAPER = 7;
    private static final String APP_TOKEN = "token-de-la-app";
    private static final String ADMIN_TOKEN = "token-del-backoffice";
    private static final String STRAY_TOKEN = "token-sin-papel";
    private static final String WEEK = "/jcfit-api/v1/weeks/2026-10-15";
    private static final String ADMIN_EXERCISES = "/jcfit-api/v1/admin/exercises";
    private static final String TOUCH = "UPDATE jcfit_tokens SET last_used_at = now()";
    private static final String UNAUTHORIZED_BODY =
            "{\"error\":\"unauthorized\",\"message\":\"Falta la sesión o ha caducado; vuelve a entrar\"}";
    private static final String FORBIDDEN_BODY =
            "{\"error\":\"forbidden\",\"message\":\"Esta cuenta no tiene acceso aquí\"}";

    private JcfitRecordingJdbcTemplate jdbc;
    private int appUserId;
    private int adminUserId;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        appUserId = CRIS;
        adminUserId = MAGOMEZ;
    }

    @Test
    void withoutTokenIsUnauthorizedWithTheContractBody() throws Exception {
        for (String uri : new String[]{WEEK, ADMIN_EXERCISES}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = run(new MockHttpServletRequest("GET", uri), response);

            assertThat(chain.getRequest()).as(uri).isNull();
            assertThat(response.getStatus()).as(uri).isEqualTo(401);
            assertThat(response.getContentType()).startsWith("application/json");
            assertThat(response.getContentAsString()).isEqualTo(UNAUTHORIZED_BODY);
        }
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
        request.addHeader("Authorization", "Basic " + APP_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        run(request, response);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void appTokenPassesOutsideAdminWithTheUserAsAttribute() throws Exception {
        session(APP_TOKEN, CRIS, "Cris");
        MockHttpServletRequest request = withToken("GET", WEEK, APP_TOKEN);

        MockFilterChain chain = run(request, new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(JcfitCurrentUser.ATTRIBUTE))
                .isEqualTo(new JcfitCurrentUser(CRIS, "Cris", JcfitSha256.hex(APP_TOKEN)));
    }

    @Test
    void backofficeTokenPassesUnderAdminWithTheUserAsAttribute() throws Exception {
        session(ADMIN_TOKEN, MAGOMEZ, "Magomez");
        MockHttpServletRequest request = withToken("GET", ADMIN_EXERCISES, ADMIN_TOKEN);

        MockFilterChain chain = run(request, new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(JcfitCurrentUser.ATTRIBUTE))
                .isEqualTo(new JcfitCurrentUser(MAGOMEZ, "Magomez", JcfitSha256.hex(ADMIN_TOKEN)));
    }

    @Test
    void appTokenUnderAdminIsForbidden() throws Exception {
        session(APP_TOKEN, CRIS, "Cris");
        for (String uri : new String[]{ADMIN_EXERCISES, "/jcfit-api/v1/admin/program", "/jcfit-api/v1/admin",
                "/jcfit-api/v1/admin/notifications", "/jcfit-api/v1/admin/progress/sessions",
                "/jcfit-api/v1/admin/auth/logout"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = run(withToken("POST", uri, APP_TOKEN), response);

            assertThat(chain.getRequest()).as(uri).isNull();
            assertThat(response.getStatus()).as(uri).isEqualTo(403);
            assertThat(response.getContentType()).startsWith("application/json");
            assertThat(response.getContentAsString()).as(uri).isEqualTo(FORBIDDEN_BODY);
        }
    }

    @Test
    void backofficeTokenOutsideAdminIsForbidden() throws Exception {
        session(ADMIN_TOKEN, MAGOMEZ, "Magomez");
        for (String uri : new String[]{WEEK, "/jcfit-api/v1/measurements", "/jcfit-api/v1/devices",
                "/jcfit-api/v1/auth/logout", "/jcfit-api/v1/administrator"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = run(withToken("GET", uri, ADMIN_TOKEN), response);

            assertThat(chain.getRequest()).as(uri).isNull();
            assertThat(response.getStatus()).as(uri).isEqualTo(403);
            assertThat(response.getContentAsString()).as(uri).isEqualTo(FORBIDDEN_BODY);
        }
    }

    @Test
    void tokenOfAnAccountWithoutAnyRoleLeftIsUnauthorizedEverywhere() throws Exception {
        session(STRAY_TOKEN, OTHER_ESCAPER, "Pepe");
        for (String uri : new String[]{WEEK, ADMIN_EXERCISES}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = run(withToken("GET", uri, STRAY_TOKEN), response);

            assertThat(chain.getRequest()).as(uri).isNull();
            assertThat(response.getStatus()).as(uri).isEqualTo(401);
            assertThat(response.getContentAsString()).isEqualTo(UNAUTHORIZED_BODY);
        }
    }

    @Test
    void anAccountWithBothRolesEntersBothSites() throws Exception {
        appUserId = MAGOMEZ;
        session(ADMIN_TOKEN, MAGOMEZ, "Magomez");

        assertThat(run(withToken("GET", WEEK, ADMIN_TOKEN), new MockHttpServletResponse()).getRequest()).isNotNull();
        assertThat(run(withToken("GET", ADMIN_EXERCISES, ADMIN_TOKEN), new MockHttpServletResponse()).getRequest())
                .isNotNull();
    }

    @Test
    void bothLoginsNeedNoToken() throws Exception {
        for (String uri : new String[]{"/jcfit-api/v1/auth/login", "/jcfit-api/v1/admin/auth/login"}) {
            MockFilterChain chain = run(new MockHttpServletRequest("POST", uri), new MockHttpServletResponse());
            assertThat(chain.getRequest()).as(uri).isNotNull();
        }
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void logoutsAndOtherMethodsOnTheLoginsDoNeedIt() throws Exception {
        for (String[] request : new String[][]{{"POST", "/jcfit-api/v1/auth/logout"},
                {"GET", "/jcfit-api/v1/auth/login"}, {"POST", "/jcfit-api/v1/admin/auth/logout"},
                {"GET", "/jcfit-api/v1/admin/auth/login"}}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            run(new MockHttpServletRequest(request[0], request[1]), response);
            assertThat(response.getStatus()).as(request[1]).isEqualTo(401);
        }
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
    void disguisedAdminRoutesAreStillForbiddenToTheAppAccount() throws Exception {
        session(APP_TOKEN, CRIS, "Cris");
        for (String uri : new String[]{"/jcfit-api/v1;x=1/admin/exercises", "/jcfit-api//v1/admin/exercises",
                "/jcfit-api/%761/admin/exercises", "/jcfit-api/v1/%61dmin/program",
                "/jcfit-api/v1/admin;x=1/notifications", "/jcfit-api/v1//admin/progress/sessions"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = run(withToken("GET", uri, APP_TOKEN), response);
            assertThat(chain.getRequest()).as(uri).isNull();
            assertThat(response.getStatus()).as(uri).isEqualTo(403);
        }
    }

    @Test
    void disguisedAdminLoginIsStillTheLoginAndDisguisedAppRoutesAreForbiddenToTheBackoffice() throws Exception {
        MockFilterChain login = run(new MockHttpServletRequest("POST", "/jcfit-api/v1;x=1/admin//auth/login"),
                new MockHttpServletResponse());
        assertThat(login.getRequest()).isNotNull();

        session(ADMIN_TOKEN, MAGOMEZ, "Magomez");
        MockHttpServletResponse response = new MockHttpServletResponse();
        run(withToken("GET", "/jcfit-api/v1;x=1/weeks/2026-10-15", ADMIN_TOKEN), response);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    /** "..": whichever site it resolves to, it is never let through, not even with a live token. */
    @Test
    void dotSegmentsAreNeverLetThrough() throws Exception {
        session(APP_TOKEN, CRIS, "Cris");
        session(ADMIN_TOKEN, MAGOMEZ, "Magomez");
        for (String uri : new String[]{"/jcfit-api/v1/weeks/../admin/program", "/jcfit-api/v1/admin/../weeks/x",
                "/jcfit-api/v1/./admin/exercises", "/escapers-api/../jcfit-api/v1/admin/exercises"}) {
            for (String token : new String[]{APP_TOKEN, ADMIN_TOKEN}) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                MockFilterChain chain = run(withToken("GET", uri, token), response);
                assertThat(chain.getRequest()).as(uri).isNull();
                assertThat(response.getStatus()).as(uri).isEqualTo(401);
            }
        }
    }

    @Test
    void databaseFailureIsAServerErrorWithoutDetails() throws Exception {
        jdbc.fail(TOUCH, new DataAccessResourceFailureException("connection refused to db.internal"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = run(withToken("GET", WEEK, APP_TOKEN), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
                .isEqualTo("{\"error\":\"server_error\",\"message\":\"Error interno, inténtalo más tarde\"}");
    }

    /** The fake answers every TOUCH with one row, so each test sets up a single session. */
    private void session(String token, int userId, String name) {
        jdbc.rows(TOUCH, row("id", userId, "name", name, "token_hash", JcfitSha256.hex(token)));
    }

    private MockFilterChain run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        JcfitAuthService service = new JcfitAuthService(new JcfitUserDao(jdbc), new JcfitTokenDao(jdbc), HASHER,
                appUserId, adminUserId);
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
