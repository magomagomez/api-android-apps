package com.magomez.androidapps.jctravels;

import com.magomez.androidapps.jctravels.config.ApiKeyFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Writes to travels-api need the X-Api-Key header; reads stay public. While the flag is
 * off a missing key is only logged, so the Cordova app keeps working until it is retired.
 */
class ApiKeyFilterTest {

    private static final String KEY = "test-key-0123456789";
    private static final String MONUMENT = "/travels-api/v1/monuments/51";

    @Test
    void readsNeverNeedAKey() throws Exception {
        Result result = run(new ApiKeyFilter(KEY, true), "GET", MONUMENT, null);
        assertThat(result.passedThrough()).isTrue();
    }

    @Test
    void writeWithoutKeyPassesWhileNotEnforced() throws Exception {
        Result result = run(new ApiKeyFilter(KEY, false), "PUT", MONUMENT, null);
        assertThat(result.passedThrough()).isTrue();
    }

    @Test
    void writeWithoutKeyIsRejectedWhenEnforced() throws Exception {
        Result result = run(new ApiKeyFilter(KEY, true), "PUT", MONUMENT, null);
        assertThat(result.passedThrough()).isFalse();
        assertThat(result.status()).isEqualTo(401);
    }

    @Test
    void writeWithWrongKeyIsRejectedWhenEnforced() throws Exception {
        Result result = run(new ApiKeyFilter(KEY, true), "POST", "/travels-api/v1/cities", "wrong");
        assertThat(result.status()).isEqualTo(401);
    }

    @Test
    void writeWithTheKeyPasses() throws Exception {
        Result result = run(new ApiKeyFilter(KEY, true), "POST", "/travels-api/v1/cities", KEY);
        assertThat(result.passedThrough()).isTrue();
    }

    @Test
    void enforcedWithoutConfiguredKeyRejectsEveryWrite() throws Exception {
        Result result = run(new ApiKeyFilter("", true), "PUT", MONUMENT, "");
        assertThat(result.status()).isEqualTo(401);
    }

    @Test
    void otherAppsAreNotAffected() throws Exception {
        Result result = run(new ApiKeyFilter(KEY, true), "PUT", "/escapersthings-api/v1/escape-rooms/3", null);
        assertThat(result.passedThrough()).isTrue();
    }

    @Test
    void rejectionDoesNotEchoTheKey() throws Exception {
        Result result = run(new ApiKeyFilter(KEY, true), "PUT", MONUMENT, "wrong");
        assertThat(result.body()).doesNotContain(KEY).doesNotContain("wrong");
    }

    private Result run(ApiKeyFilter filter, String method, String uri, String key) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (key != null) {
            request.addHeader(ApiKeyFilter.HEADER, key);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return new Result(chain.getRequest() != null, response.getStatus(), response.getContentAsString());
    }

    private record Result(boolean passedThrough, int status, String body) {
    }
}
