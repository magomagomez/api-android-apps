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
    private volatile String lastUserAgent;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/sparql", exchange -> {
            hits.incrementAndGet();
            lastUserAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            byte[] body = SPARQL_JSON.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
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
}
