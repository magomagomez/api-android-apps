package com.magomez.androidapps.movierec.provider.wikidata;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.movierec.provider.wikidata.dto.WikidataSparqlResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin client for the Wikidata SPARQL endpoint ({@code query.wikidata.org/sparql}),
 * same shape as {@code TmdbClient} / {@code OmdbClient} (OkHttp + Jackson).
 *
 * <p>One query: given an IMDb id, the awards a film received ({@code wdt:P166}) and the
 * ones it was nominated for ({@code wdt:P1411}), each with the "conferred by" entity and
 * the year on the statement, plus the film's release year as a fallback.
 *
 * <p>Wikidata rejects requests without a descriptive {@code User-Agent}; one is always
 * sent. Responses are held in a small bounded LRU ({@value #MAX_CACHED} entries) —
 * process-local, never persisted, catalogue data.
 */
@Component
public class WikidataClient {

    private static final String DEFAULT_ENDPOINT = "https://query.wikidata.org/sparql";
    /**
     * Wikidata's User-Agent policy wants a contact; override with
     * {@code wikidata.user-agent} to put a real e-mail / URL in.
     */
    private static final String DEFAULT_USER_AGENT =
            "movie-recommendation-engine/1.0 (https://github.com/Magomez; personal, non-commercial)";
    static final int MAX_CACHED = 5000;

    private static final String QUERY_TEMPLATE = """
            SELECT ?kind ?awardLabel ?conferrerLabel ?year ?pubyear WHERE {
              ?film wdt:P345 "%s" .
              OPTIONAL { ?film wdt:P577 ?pub . BIND(YEAR(?pub) AS ?pubyear) }
              {
                ?film p:P166 ?st . ?st ps:P166 ?award . BIND("W" AS ?kind)
                OPTIONAL { ?st pq:P585 ?d . BIND(YEAR(?d) AS ?year) }
              } UNION {
                ?film p:P1411 ?st . ?st ps:P1411 ?award . BIND("N" AS ?kind)
                OPTIONAL { ?st pq:P585 ?d . BIND(YEAR(?d) AS ?year) }
              }
              OPTIONAL { ?award wdt:P1027 ?conferrer . }
              SERVICE wikibase:label { bd:serviceParam wikibase:language "en,es" . }
            }
            LIMIT 200""";

    /**
     * HTTP/1.1 only: the shared WDQS endpoint resets HTTP/2 streams from some clients
     * ({@code stream was reset: CANCEL}). Timeout deliberately short: this signal is a
     * bonus (any failure already degrades to an empty report, never breaks the request),
     * scoring runs one of these per candidate in parallel batches, and the whole request
     * has to fit inside Heroku's hard 30s router timeout — a single slow call must not be
     * allowed to hold up its whole batch for anywhere near that long.
     */
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .protocols(List.of(Protocol.HTTP_1_1))
            .callTimeout(Duration.ofSeconds(8))
            .readTimeout(Duration.ofSeconds(8))
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final String endpoint;
    private final String userAgent;

    private final Map<String, List<WikidataAward>> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<WikidataAward>> eldest) {
                    return size() > MAX_CACHED;
                }
            });

    public WikidataClient(
            @Value("${wikidata.sparql.endpoint:" + DEFAULT_ENDPOINT + "}") String endpoint,
            @Value("${wikidata.user-agent:" + DEFAULT_USER_AGENT + "}") String userAgent) {
        this.endpoint = endpoint;
        this.userAgent = userAgent == null || userAgent.isBlank() ? DEFAULT_USER_AGENT : userAgent;
    }

    public List<WikidataAward> awardsForImdbId(String imdbId) throws IOException {
        List<WikidataAward> cached = cache.get(imdbId);
        if (cached != null) {
            return cached;
        }

        HttpUrl parsed = HttpUrl.parse(endpoint);
        if (parsed == null) {
            throw new IllegalStateException("Invalid Wikidata endpoint: " + endpoint);
        }
        HttpUrl url = parsed.newBuilder()
                .addQueryParameter("query", String.format(QUERY_TEMPLATE, sanitize(imdbId)))
                .addQueryParameter("format", "json")
                .build();

        Request request = new Request.Builder().url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/sparql-results+json")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("Wikidata request failed with HTTP " + response.code());
            }
            WikidataSparqlResponse parsedBody =
                    objectMapper.readValue(body.string(), WikidataSparqlResponse.class);
            List<WikidataAward> awards = toAwards(parsedBody);
            cache.put(imdbId, awards);
            return awards;
        }
    }

    private static List<WikidataAward> toAwards(WikidataSparqlResponse response) {
        List<WikidataAward> awards = new ArrayList<>();
        for (Map<String, WikidataSparqlResponse.Cell> row : response.rows()) {
            String awardLabel = value(row, "awardLabel");
            if (awardLabel == null) {
                continue;
            }
            awards.add(new WikidataAward(
                    "W".equals(value(row, "kind")),
                    awardLabel,
                    value(row, "conferrerLabel"),
                    parseYear(value(row, "year")),
                    parseYear(value(row, "pubyear"))));
        }
        return List.copyOf(awards);
    }

    private static String value(Map<String, WikidataSparqlResponse.Cell> row, String key) {
        WikidataSparqlResponse.Cell cell = row.get(key);
        return cell == null || cell.value() == null || cell.value().isBlank() ? null : cell.value();
    }

    private static Integer parseYear(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            int year = Integer.parseInt(raw.trim());
            return (year >= 1900 && year <= 2100) ? year : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** IMDb ids are {@code tt} + digits; strip anything else so it cannot break out of the literal. */
    private static String sanitize(String imdbId) {
        return imdbId == null ? "" : imdbId.replaceAll("[^A-Za-z0-9]", "");
    }
}
