package com.magomez.androidapps.regalos.auth;

import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.people.RegalosUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Every regalos-api route needs a live Bearer token except registering and logging in. */
class RegalosBearerTokenFilterTest {

    private static final RegalosPasswordHasher HASHER = new RegalosPasswordHasher();
    private static final int CRIS = 2;
    private static final String TOKEN = "token-de-cris";
    private static final String PEOPLE = "/regalos-api/v1/people";
    private static final String TOUCH = "UPDATE regalos_tokens SET last_used_at = now()";
    private static final String UNAUTHORIZED_BODY =
            "{\"error\":\"unauthorized\",\"message\":\"Falta la sesión o ha caducado; vuelve a entrar\"}";

    private RegalosFakeJdbc jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
    }

    @Test
    void withoutTokenIsUnauthorizedWithTheContractBody() throws Exception {
        for (String uri : new String[]{PEOPLE, "/regalos-api/v1/me/items", "/regalos-api/v1/auth/logout"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = run(new MockHttpServletRequest("GET", uri), response);

            assertThat(chain.getRequest()).as(uri).isNull();
            assertThat(response.getStatus()).as(uri).isEqualTo(401);
            assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
            assertThat(response.getContentType()).startsWith("application/json");
            assertThat(response.getContentAsString()).isEqualTo(UNAUTHORIZED_BODY);
        }
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void inventedOrRevokedTokenIsUnauthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(withToken("GET", PEOPLE, "inventado"), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(jdbc.last().args()).containsExactly(RegalosSha256.hex("inventado"));
    }

    @Test
    void otherSchemesAreUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PEOPLE);
        request.addHeader("Authorization", "Basic " + TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        run(request, response);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void aLiveTokenPassesWithTheUserAsAttribute() throws Exception {
        jdbc.rows(TOUCH, row("id", CRIS, "display_name", "Cris", "token_hash", RegalosSha256.hex(TOKEN)));
        MockHttpServletRequest request = withToken("GET", PEOPLE, TOKEN);

        MockFilterChain chain = run(request, new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(RegalosCurrentUser.ATTRIBUTE))
                .isEqualTo(new RegalosCurrentUser(CRIS, "Cris", RegalosSha256.hex(TOKEN)));
    }

    @Test
    void registerAndLoginNeedNoToken() throws Exception {
        for (String uri : new String[]{"/regalos-api/v1/auth/register", "/regalos-api/v1/auth/login",
                "/regalos-api/v1;x=1/auth//login"}) {
            MockFilterChain chain = run(new MockHttpServletRequest("POST", uri), new MockHttpServletResponse());
            assertThat(chain.getRequest()).as(uri).isNotNull();
        }
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void otherMethodsOnThePublicRoutesDoNeedIt() throws Exception {
        for (String uri : new String[]{"/regalos-api/v1/auth/register", "/regalos-api/v1/auth/login"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            run(new MockHttpServletRequest("GET", uri), response);
            assertThat(response.getStatus()).as(uri).isEqualTo(401);
        }
    }

    @Test
    void routesOutsideRegalosApiAreNotFiltered() throws Exception {
        for (String uri : new String[]{"/jcfit-api/v1/weeks", "/escapers-api/v2/escape-rooms", "/regalos-api/v1",
                "/regalos-api/v10/people"}) {
            MockFilterChain chain = run(new MockHttpServletRequest("GET", uri), new MockHttpServletResponse());
            assertThat(chain.getRequest()).as(uri).isNotNull();
        }
        assertThat(jdbc.calls).isEmpty();
    }

    /** Spring routes these to regalos controllers, so the filter must see them as regalos routes too. */
    @Test
    void disguisedRoutesAreStillFiltered() throws Exception {
        for (String uri : new String[]{"/regalos-api/v1;x=1/people", "/regalos-api//v1/people",
                "/regalos-api/%761/people", "/regalos-api/v1/auth/login;x=1/../../people",
                "/regalos-api/v1/auth/login/../../v1/people", "/jcfit-api/../regalos-api/v1/people"}) {
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

        MockFilterChain chain = run(withToken("GET", PEOPLE, TOKEN), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
                .isEqualTo("{\"error\":\"server_error\",\"message\":\"Error interno, inténtalo más tarde\"}");
    }

    private MockFilterChain run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        RegalosAuthService service = new RegalosAuthService(new RegalosUserDao(jdbc), new RegalosTokenDao(jdbc),
                HASHER, new LoginThrottle(Clock.systemUTC()), "REGALOS-7Q4K");
        MockFilterChain chain = new MockFilterChain();
        new RegalosBearerTokenFilter(service).doFilter(request, response, chain);
        return chain;
    }

    private static MockHttpServletRequest withToken(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
