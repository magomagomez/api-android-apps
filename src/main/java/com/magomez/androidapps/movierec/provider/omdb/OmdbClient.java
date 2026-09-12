package com.magomez.androidapps.movierec.provider.omdb;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin HTTP client for the OMDb API, same shape as {@code TmdbClient} (OkHttp +
 * Jackson, both already on the classpath).
 *
 * <p>The API key is read from configuration ({@code omdb.api.key} in
 * {@code application.properties}). It is never logged or returned in responses.
 *
 * <p>Responses are held in a small bounded in-memory LRU ({@value #MAX_CACHED} entries):
 * within a request the same movie can be looked up by more than one enricher / provider
 * (ratings, then awards). Process-local, never written anywhere, gone on restart — OMDb
 * catalogue data, not user data.
 */
@Component
public class OmdbClient {

    private static final String DEFAULT_BASE_URL = "https://www.omdbapi.com/";
    static final int MAX_CACHED = 5000;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** Access-ordered LRU of responses by IMDb id; see the class javadoc. */
    private final Map<String, OmdbResponse> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, OmdbResponse> eldest) {
                    return size() > MAX_CACHED;
                }
            });

    private final String apiKey;
    private final String baseUrl;

    public OmdbClient(
            @Value("${omdb.api.key}") String apiKey,
            @Value("${omdb.api.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    /**
     * {@code GET /?apikey=...&i={imdbId}} — strict lookup by IMDb ID, never by title.
     *
     * @throws IOException when OMDb cannot be reached or answers with a non-2xx status
     */
    public OmdbResponse byImdbId(String imdbId) throws IOException {
        OmdbResponse cached = cache.get(imdbId);
        if (cached != null) {
            return cached;
        }

        HttpUrl parsed = HttpUrl.parse(baseUrl);
        if (parsed == null) {
            throw new IllegalStateException("Invalid OMDb base url: " + baseUrl);
        }
        HttpUrl url = parsed.newBuilder()
                .addQueryParameter("apikey", apiKey)
                .addQueryParameter("i", imdbId)
                .build();

        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("OMDb request failed with HTTP " + response.code());
            }
            OmdbResponse parsedResponse = objectMapper.readValue(body.string(), OmdbResponse.class);
            cache.put(imdbId, parsedResponse);
            return parsedResponse;
        }
    }
}
