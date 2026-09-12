package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.provider.omdb.OmdbClient;
import com.magomez.androidapps.movierec.provider.omdb.dto.OmdbResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OmdbClient} against a throwaway in-process HTTP server. Covers the response
 * LRU: a repeat IMDb id is served without a new request.
 */
class OmdbClientTest {

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            byte[] body = "{\"Response\":\"True\",\"Awards\":\"Won 1 Oscar. 5 wins & 3 nominations total\"}"
                    .getBytes(StandardCharsets.UTF_8);
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
    void aRepeatLookupIsServedFromTheCache() throws IOException {
        OmdbClient client = new OmdbClient("test-key",
                "http://localhost:" + server.getAddress().getPort() + "/");

        OmdbResponse first = client.byImdbId("tt0111161");
        OmdbResponse again = client.byImdbId("tt0111161");
        client.byImdbId("tt0068646"); // a different id does hit the network

        assertThat(first.awards()).isEqualTo("Won 1 Oscar. 5 wins & 3 nominations total");
        assertThat(again).isSameAs(first);
        assertThat(hits).hasValue(2);
    }
}
