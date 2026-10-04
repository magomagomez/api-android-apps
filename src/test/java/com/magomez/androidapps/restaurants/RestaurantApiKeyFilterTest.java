package com.magomez.androidapps.restaurants;

import com.magomez.androidapps.restaurants.config.RestaurantApiKeyFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Writes to restaurant-api always need X-Api-Key: there is no old app to keep working. */
class RestaurantApiKeyFilterTest {

    private static final String KEY = "gourmet-test-key-0123";
    private static final String RESTAURANTS = "/restaurant-api/v1/restaurants";

    @Test
    void readsNeverNeedAKey() throws Exception {
        assertThat(run(KEY, "GET", RESTAURANTS, null).passedThrough()).isTrue();
    }

    @Test
    void writeWithoutKeyIsRejected() throws Exception {
        Result result = run(KEY, "POST", RESTAURANTS, null);
        assertThat(result.passedThrough()).isFalse();
        assertThat(result.status()).isEqualTo(401);
    }

    @Test
    void writeWithWrongKeyIsRejected() throws Exception {
        assertThat(run(KEY, "DELETE", RESTAURANTS + "/4", "wrong").status()).isEqualTo(401);
    }

    @Test
    void writeWithTheKeyPasses() throws Exception {
        assertThat(run(KEY, "PUT", RESTAURANTS + "/4/visit", KEY).passedThrough()).isTrue();
    }

    @Test
    void withoutConfiguredKeyEveryWriteIsRejected() throws Exception {
        assertThat(run("", "POST", RESTAURANTS, "").status()).isEqualTo(401);
    }

    @Test
    void otherApisAreNotAffected() throws Exception {
        assertThat(run(KEY, "POST", "/friki-api/v1/funkos", null).passedThrough()).isTrue();
    }

    @Test
    void rejectionDoesNotEchoTheKey() throws Exception {
        assertThat(run(KEY, "POST", RESTAURANTS, "wrong").body()).doesNotContain(KEY).doesNotContain("wrong");
    }

    private Result run(String configuredKey, String method, String uri, String key) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (key != null) {
            request.addHeader(RestaurantApiKeyFilter.HEADER, key);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        new RestaurantApiKeyFilter(configuredKey).doFilter(request, response, chain);
        return new Result(chain.getRequest() != null, response.getStatus(), response.getContentAsString());
    }

    private record Result(boolean passedThrough, int status, String body) {
    }
}
