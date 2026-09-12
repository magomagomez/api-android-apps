package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.provider.tmdb.TmdbClient;
import com.magomez.androidapps.movierec.provider.tmdb.dto.TmdbMovieDetails;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TmdbClient} against a throwaway in-process HTTP server (no real TMDB, no mocking
 * framework). Covers the movie-details LRU: a repeat id is served without a new request.
 */
class TmdbClientTest {

    private HttpServer server;
    private final AtomicInteger movieDetailHits = new AtomicInteger();
    private final java.util.Map<Integer, AtomicInteger> hitsPerId = new java.util.concurrent.ConcurrentHashMap<>();
    /** Ids in this set fail every request; used to test giving up after the retry. */
    private final java.util.Set<Integer> alwaysFail = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Ids in this set fail only their first request; used to test the retry succeeding. */
    private final java.util.Set<Integer> failOnce = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/movie/", exchange -> {
            movieDetailHits.incrementAndGet();
            int id = Integer.parseInt(exchange.getRequestURI().getPath().substring("/movie/".length()));
            int hit = hitsPerId.computeIfAbsent(id, k -> new AtomicInteger()).incrementAndGet();

            if (alwaysFail.contains(id) || (failOnce.contains(id) && hit == 1)) {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }
            byte[] body = ("{\"id\":" + id + ",\"title\":\"Movie " + id + "\"}")
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

    private TmdbClient client() {
        int port = server.getAddress().getPort();
        return new TmdbClient("test-key", "http://localhost:" + port + "/");
    }

    @Test
    void movieDetailsAreFetchedOncePerIdThenServedFromTheCache() throws IOException {
        TmdbClient client = client();

        TmdbMovieDetails first = client.movieDetails(42);
        TmdbMovieDetails again = client.movieDetails(42);
        client.movieDetails(7); // a different id does hit the network

        assertThat(first.id()).isEqualTo(42);
        assertThat(again).isSameAs(first);
        assertThat(movieDetailHits).hasValue(2); // id 42 once, id 7 once
    }

    @Test
    void aTransientFailureIsRetriedOnceAndThenSucceeds() throws IOException {
        failOnce.add(55);
        TmdbClient client = client();

        TmdbMovieDetails details = client.movieDetails(55);

        assertThat(details.id()).isEqualTo(55);
        assertThat(hitsPerId.get(55)).hasValue(2); // first attempt failed, second succeeded
    }

    @Test
    void givesUpAfterTheRetryAlsoFails() {
        alwaysFail.add(66);
        TmdbClient client = client();

        assertThatThrownBy(() -> client.movieDetails(66)).isInstanceOf(IOException.class);

        assertThat(hitsPerId.get(66)).hasValue(2); // exactly one retry, then it gives up
    }
}
