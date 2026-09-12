package com.magomez.androidapps.movierec.provider.tmdb;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbPersonDetails;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbSearchResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin HTTP client for the TMDB REST API.
 *
 * <p>The API key is read from configuration ({@code tmdb.api.key} in
 * {@code application.properties}). It is never logged or returned in responses.
 *
 * <p>{@link #movieDetails(int)} responses are held in a small bounded in-memory LRU
 * ({@value #MAX_CACHED_DETAILS} entries). Identifying a candidate already fetches a
 * movie's details, and the rating-enrichment step then asks for the very same id; within
 * one request the library and the candidate list also revisit popular titles. The cache
 * removes those repeat round-trips. It is process-local, never written anywhere, and
 * gone on restart — TMDB catalogue data, not user data.
 *
 * <p>Warming up the Letterboxd library fans out ~12 concurrent requests
 * ({@link com.magomez.androidapps.movierec.support.ExternalCallExecutor}); under that load
 * a handful of individual calls occasionally time out even when TMDB itself is healthy. A
 * transient failure is retried once, after a short pause, before giving up — the caller
 * (per-entry isolated already) sees an {@link IOException} only if both attempts fail.
 */
@Component
public class TmdbClient {

    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);

    static final int MAX_CACHED_DETAILS = 5000;
    /** One retry: the first attempt plus one more after a transient failure. */
    private static final int MAX_ATTEMPTS = 2;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(300);

    private static final String DEFAULT_BASE_URL = "https://api.themoviedb.org/3/";
    /** Language for the movie <em>details</em> we turn into a domain {@code Movie}. */
    private static final String LANGUAGE = "es-ES";
    /**
     * Language for the <em>search</em> step. English so the {@code title} in results is
     * the international title (which is what Letterboxd exports and users type), making
     * title matching reliable; the result set itself is language-independent.
     */
    private static final String SEARCH_LANGUAGE = "en-US";

    /**
     * Deliberately short: TMDB normally answers in well under a second, so a hang is
     * almost always going to stay hung — failing fast and retrying costs far less overall
     * than waiting a long time twice on the same bad connection.
     */
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** Access-ordered LRU of movie-details responses by TMDB id; see the class javadoc. */
    private final Map<Integer, TmdbMovieDetails> detailsCache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, TmdbMovieDetails> eldest) {
                    return size() > MAX_CACHED_DETAILS;
                }
            });

    private final String apiKey;
    private final String baseUrl;

    public TmdbClient(
            @Value("${tmdb.api.key}") String apiKey,
            @Value("${tmdb.api.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    /** {@code GET /search/movie}. When {@code year} is not null it is passed as a filter. */
    public TmdbSearchResponse searchMovies(String title, Integer year) throws IOException {
        HttpUrl.Builder builder = urlBuilder("search/movie")
                .addQueryParameter("query", title)
                .addQueryParameter("include_adult", "false")
                .addQueryParameter("language", SEARCH_LANGUAGE);
        if (year != null) {
            builder.addQueryParameter("year", String.valueOf(year));
        }
        return get(builder.build(), TmdbSearchResponse.class);
    }

    /** {@code GET /movie/{id}?append_to_response=credits,similar}. Served from the LRU on a repeat id. */
    public TmdbMovieDetails movieDetails(int id) throws IOException {
        TmdbMovieDetails cached = detailsCache.get(id);
        if (cached != null) {
            return cached;
        }
        HttpUrl url = urlBuilder("movie/" + id)
                .addQueryParameter("language", LANGUAGE)
                .addQueryParameter("append_to_response", "credits,similar")
                .build();
        TmdbMovieDetails fetched = get(url, TmdbMovieDetails.class);
        detailsCache.put(id, fetched);
        return fetched;
    }

    /** {@code GET /person/{id}} — used to romanize a name credited only in a non-Latin script. */
    public TmdbPersonDetails personDetails(int id) throws IOException {
        HttpUrl url = urlBuilder("person/" + id)
                .addQueryParameter("language", LANGUAGE)
                .build();
        return get(url, TmdbPersonDetails.class);
    }

    private HttpUrl.Builder urlBuilder(String path) {
        HttpUrl parsed = HttpUrl.parse(baseUrl + path);
        if (parsed == null) {
            throw new IllegalStateException("Invalid TMDB base url: " + baseUrl);
        }
        return parsed.newBuilder().addQueryParameter("api_key", apiKey);
    }

    private <T> T get(HttpUrl url, Class<T> type) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return attempt(url, type);
            } catch (IOException e) {
                lastFailure = e;
                if (attempt < MAX_ATTEMPTS) {
                    log.debug("TMDB request to {} failed ({}), retrying once: {}",
                            url.encodedPath(), e.getClass().getSimpleName(), e.getMessage());
                    sleep(RETRY_BACKOFF);
                }
            }
        }
        throw lastFailure;
    }

    private <T> T attempt(HttpUrl url, Class<T> type) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("TMDB request failed with HTTP " + response.code());
            }
            return objectMapper.readValue(body.string(), type);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
