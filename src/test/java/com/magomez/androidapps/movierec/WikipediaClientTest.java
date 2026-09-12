package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.provider.festival.wikipedia.WikipediaClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WikipediaClient} against a throwaway in-process HTTP server: unwraps the
 * {@code action=parse} JSON, treats a missing page as empty, sends a User-Agent, caches.
 */
class WikipediaClientTest {

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private volatile String lastUserAgent;
    private volatile String responseJson;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/w/api.php", exchange -> {
            hits.incrementAndGet();
            lastUserAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            byte[] body = responseJson.getBytes(StandardCharsets.UTF_8);
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

    private WikipediaClient client() {
        return new WikipediaClient(
                "http://localhost:" + server.getAddress().getPort() + "/w/api.php",
                "movie-recommendation-engine-test/1.0");
    }

    @Test
    void returnsTheRenderedHtmlAndCachesTheLookup() throws IOException {
        responseJson = "{\"parse\":{\"title\":\"2026 Sundance Film Festival\","
                + "\"text\":\"<div class=\\\"mw-parser-output\\\"><p>hi</p></div>\"}}";
        WikipediaClient client = client();

        Optional<String> first = client.pageHtml("2026 Sundance Film Festival");
        Optional<String> again = client.pageHtml("2026 Sundance Film Festival");

        assertThat(first).contains("<div class=\"mw-parser-output\"><p>hi</p></div>");
        assertThat(again).isEqualTo(first);
        assertThat(hits).hasValue(1);
        assertThat(lastUserAgent).contains("movie-recommendation-engine");
    }

    @Test
    void aMissingPageIsAnEmptyOptional() throws IOException {
        responseJson = "{\"error\":{\"code\":\"missingtitle\",\"info\":\"The page you specified doesn't exist.\"}}";

        assertThat(client().pageHtml("2099 Nonexistent Film Festival")).isEmpty();
    }
}
