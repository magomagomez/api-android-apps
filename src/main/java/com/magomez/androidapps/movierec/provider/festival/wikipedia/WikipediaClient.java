package com.magomez.androidapps.movierec.provider.festival.wikipedia;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.movierec.provider.festival.wikipedia.dto.WikipediaParseResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Thin client for the MediaWiki {@code action=parse} API on English Wikipedia. Wikipedia
 * content is open (CC BY-SA) and this is its public API — not scraping an arbitrary site.
 *
 * <p>{@link #pageHtml(String)} returns the rendered HTML of an article (redirects
 * followed), or {@link Optional#empty()} when there is no such page. Responses are held
 * in a small bounded LRU ({@value #MAX_CACHED}); a descriptive User-Agent is always sent.
 */
@Component
public class WikipediaClient {

    private static final String DEFAULT_ENDPOINT = "https://en.wikipedia.org/w/api.php";
    private static final String DEFAULT_USER_AGENT =
            "movie-recommendation-engine/1.0 (https://github.com/Magomez; personal, non-commercial)";
    static final int MAX_CACHED = 2000;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(20))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final String endpoint;
    private final String userAgent;

    private final Map<String, Optional<String>> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Optional<String>> eldest) {
                    return size() > MAX_CACHED;
                }
            });

    public WikipediaClient(
            @Value("${wikipedia.api.endpoint:" + DEFAULT_ENDPOINT + "}") String endpoint,
            @Value("${wikipedia.user-agent:" + DEFAULT_USER_AGENT + "}") String userAgent) {
        this.endpoint = endpoint;
        this.userAgent = userAgent == null || userAgent.isBlank() ? DEFAULT_USER_AGENT : userAgent;
    }

    public Optional<String> pageHtml(String pageTitle) throws IOException {
        Optional<String> cached = cache.get(pageTitle);
        if (cached != null) {
            return cached;
        }

        HttpUrl parsed = HttpUrl.parse(endpoint);
        if (parsed == null) {
            throw new IllegalStateException("Invalid Wikipedia endpoint: " + endpoint);
        }
        HttpUrl url = parsed.newBuilder()
                .addQueryParameter("action", "parse")
                .addQueryParameter("page", pageTitle)
                .addQueryParameter("prop", "text")
                .addQueryParameter("redirects", "1")
                .addQueryParameter("formatversion", "2")
                .addQueryParameter("format", "json")
                .build();

        Request request = new Request.Builder().url(url)
                .header("User-Agent", userAgent)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (response.code() == 404) {
                return cacheAndReturn(pageTitle, Optional.empty());
            }
            if (!response.isSuccessful() || body == null) {
                throw new IOException("Wikipedia request failed with HTTP " + response.code());
            }
            WikipediaParseResponse parsedBody =
                    objectMapper.readValue(body.string(), WikipediaParseResponse.class);
            return cacheAndReturn(pageTitle, Optional.ofNullable(parsedBody.html()));
        }
    }

    private Optional<String> cacheAndReturn(String key, Optional<String> value) {
        cache.put(key, value);
        return value;
    }
}
