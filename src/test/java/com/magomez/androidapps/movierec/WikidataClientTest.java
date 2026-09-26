package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.provider.wikidata.WikidataAward;
import com.magomez.androidapps.movierec.provider.wikidata.WikidataClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WikidataClient} against a throwaway in-process HTTP server: parses the SPARQL
 * JSON, sends a User-Agent, and serves a repeat lookup from the cache.
 */
class WikidataClientTest {

    private static final String SPARQL_JSON = """
            {"head":{"vars":["kind","awardLabel","conferrerLabel","year","pubyear"]},
             "results":{"bindings":[
               {"kind":{"value":"W"},"awardLabel":{"value":"Palme d'Or"},
                "year":{"value":"2024"},"pubyear":{"value":"2024"}},
               {"kind":{"value":"N"},"awardLabel":{"value":"Golden Bear"},
                "conferrerLabel":{"value":"Berlin International Film Festival"},
                "pubyear":{"value":"2024"}}
             ]}}""";

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    /** Status codes to serve, one per request, in order; the last one repeats past the end. */
    private volatile java.util.List<Integer> statusSequence = List.of(200);
    private volatile String lastUserAgent;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/sparql", exchange -> {
            int attempt = hits.getAndIncrement();
            lastUserAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            java.util.List<Integer> sequence = statusSequence;
            int status = sequence.get(Math.min(attempt, sequence.size() - 1));
            byte[] body = (status == 200 ? SPARQL_JSON : "").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void parsesAwardsAndNominationsAndCachesTheLookup() throws IOException {
        WikidataClient client = new WikidataClient(
                "http://localhost:" + server.getAddress().getPort() + "/sparql",
                "movie-recommendation-engine-test/1.0");

        List<WikidataAward> first = client.awardsForImdbId("tt1234567");
        List<WikidataAward> again = client.awardsForImdbId("tt1234567");

        assertThat(first).containsExactly(
                new WikidataAward(true, "Palme d'Or", null, 2024, 2024),
                new WikidataAward(false, "Golden Bear", "Berlin International Film Festival", null, 2024));
        assertThat(again).isSameAs(first);
        assertThat(hits).hasValue(1); // second call served from cache
        assertThat(lastUserAgent).contains("movie-recommendation-engine");
    }

    @Test
    void aFailedFirstAttemptIsRetriedOnceAndSucceeds() throws IOException {
        statusSequence = List.of(500, 200); // cold query times out server-side once, then serves fine
        WikidataClient client = new WikidataClient(
                "http://localhost:" + server.getAddress().getPort() + "/sparql",
                "movie-recommendation-engine-test/1.0");

        List<WikidataAward> awards = client.awardsForImdbId("tt1234567");

        assertThat(awards).hasSize(2);
        assertThat(hits).hasValue(2); // first attempt failed, retry succeeded
    }

    @Test
    void twoConsecutiveFailuresPropagateTheException() {
        statusSequence = List.of(500);
        WikidataClient client = new WikidataClient(
                "http://localhost:" + server.getAddress().getPort() + "/sparql",
                "movie-recommendation-engine-test/1.0");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> client.awardsForImdbId("tt1234567")))
                .isInstanceOf(IOException.class);
        assertThat(hits).hasValue(2); // one attempt plus one retry, both failed
    }
}
