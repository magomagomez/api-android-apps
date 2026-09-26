package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.FilmScreening;
import com.magomez.androidapps.movierec.provider.sitges.SitgesScheduleClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SitgesScheduleClient} against a throwaway in-process HTTP server standing in for
 * the festival's JSON API: resolves the manifest's pages, cross-references venue names,
 * and never lets one bad page or the venue lookup take the whole fetch down.
 */
class SitgesScheduleClientTest {

    private static final String LOCATIONS_JSON =
            "{\"locations\":[{\"id\":\"392-location\",\"name\":{\"es\":\"Auditori Meliá\"}}]}";

    private HttpServer server;
    private final Map<String, String> responses = new ConcurrentHashMap<>();
    private final Map<String, Integer> statusCodes = new ConcurrentHashMap<>();
    private volatile String lastUserAgent;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        registerContext("/public/api/films/locations.json");
        registerContext("/api/v1/se/films/2026/sessions/manifest");
        registerContext("/public/api/se/films/2026/sessions.json");
        registerContext("/public/api/se/films/2026/sessions2.json");
        registerContext("/api/v1/se/films/2026/films/manifest");
        registerContext("/public/api/se/films/2026/films.json");
        server.start();
        responses.put("/public/api/films/locations.json", LOCATIONS_JSON);
    }

    private void registerContext(String path) {
        server.createContext(path, exchange -> {
            lastUserAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            int status = statusCodes.getOrDefault(path, 200);
            byte[] body = responses.getOrDefault(path, "{}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private SitgesScheduleClient client() {
        return new SitgesScheduleClient(baseUrl(), 2026, "movie-recommendation-engine-test/1.0");
    }

    private void manifestWithOnePage() {
        responses.put("/api/v1/se/films/2026/sessions/manifest",
                "{\"pages\":[{\"url\":\"" + baseUrl() + "/public/api/se/films/2026/sessions.json\"}]}");
    }

    @Test
    void resolvesTitleDateTimesAndVenueName() throws IOException {
        manifestWithOnePage();
        responses.put("/public/api/se/films/2026/sessions.json", "{\"sessions\":[{"
                + "\"name\":{\"es\":\"Buddy\"},"
                + "\"start_date\":\"2026-10-09T19:00:00\","
                + "\"end_date\":\"2026-10-09T20:41:00\","
                + "\"locations\":[\"392-location\"]}]}");

        List<FilmScreening> screenings = client().screenings();

        assertThat(screenings).containsExactly(new FilmScreening(
                "Buddy", LocalDate.of(2026, 10, 9), LocalTime.of(19, 0), LocalTime.of(20, 41), "Auditori Meliá"));
    }

    @Test
    void sendsAUserAgent() throws IOException {
        manifestWithOnePage();
        responses.put("/public/api/se/films/2026/sessions.json", "{\"sessions\":[]}");

        client().screenings();

        assertThat(lastUserAgent).isEqualTo("movie-recommendation-engine-test/1.0");
    }

    @Test
    void aSessionWithNoStartDateIsSkipped() throws IOException {
        manifestWithOnePage();
        responses.put("/public/api/se/films/2026/sessions.json",
                "{\"sessions\":[{\"name\":{\"es\":\"No Date\"},\"locations\":[]}]}");

        assertThat(client().screenings()).isEmpty();
    }

    @Test
    void aFailingPageIsSkippedInsteadOfFailingTheWholeFetch() throws IOException {
        responses.put("/api/v1/se/films/2026/sessions/manifest", "{\"pages\":["
                + "{\"url\":\"" + baseUrl() + "/public/api/se/films/2026/sessions.json\"},"
                + "{\"url\":\"" + baseUrl() + "/public/api/se/films/2026/sessions2.json\"}]}");
        responses.put("/public/api/se/films/2026/sessions.json", "{\"sessions\":[{"
                + "\"name\":{\"es\":\"Good Page\"},\"start_date\":\"2026-10-09T19:00:00\",\"locations\":[]}]}");
        statusCodes.put("/public/api/se/films/2026/sessions2.json", 500);

        List<FilmScreening> screenings = client().screenings();

        assertThat(screenings).extracting(FilmScreening::title).containsExactly("Good Page");
    }

    @Test
    void aDoubleBillSessionProducesOneScreeningPerFilmInsteadOfTheStrandName() throws IOException {
        manifestWithOnePage();
        responses.put("/api/v1/se/films/2026/films/manifest",
                "{\"pages\":[{\"url\":\"" + baseUrl() + "/public/api/se/films/2026/films.json\"}]}");
        responses.put("/public/api/se/films/2026/films.json", "{\"films\":["
                + "{\"id\":\"14307-film\",\"international_title\":\"Full Phil\"},"
                + "{\"id\":\"14246-film\",\"international_title\":\"Vertiginous\"}]}");
        // A themed double bill: its own display name never mentions either film.
        responses.put("/public/api/se/films/2026/sessions.json", "{\"sessions\":[{"
                + "\"name\":{\"es\":\"La nit (golfa) de Quentin Dupieux\"},"
                + "\"start_date\":\"2026-10-13T23:15:00\",\"end_date\":\"2026-10-14T01:41:00\","
                + "\"locations\":[],\"films\":[\"14307-film\",\"14246-film\"]}]}");

        List<FilmScreening> screenings = client().screenings();

        assertThat(screenings).extracting(FilmScreening::title).containsExactly("Full Phil", "Vertiginous");
        assertThat(screenings).allMatch(s -> s.date().equals(LocalDate.of(2026, 10, 13))
                && s.startTime().equals(LocalTime.of(23, 15)));
    }

    @Test
    void resolvesTheFilmsOriginalLanguageTitleWhenItDiffersFromTheDisplayTitle() throws IOException {
        manifestWithOnePage();
        responses.put("/api/v1/se/films/2026/films/manifest",
                "{\"pages\":[{\"url\":\"" + baseUrl() + "/public/api/se/films/2026/films.json\"}]}");
        responses.put("/public/api/se/films/2026/films.json", "{\"films\":["
                + "{\"id\":\"14246-film\",\"international_title\":\"Vertiginous\",\"original_title\":\"Le Vertige\"}]}");
        responses.put("/public/api/se/films/2026/sessions.json", "{\"sessions\":[{"
                + "\"name\":{\"es\":\"Vertiginous\"},\"start_date\":\"2026-10-13T22:00:00\","
                + "\"locations\":[],\"films\":[\"14246-film\"]}]}");

        List<FilmScreening> screenings = client().screenings();

        assertThat(screenings).singleElement().satisfies(s -> {
            assertThat(s.title()).isEqualTo("Vertiginous");
            assertThat(s.originalTitle()).isEqualTo("Le Vertige");
        });
    }

    @Test
    void anOriginalTitleEqualToTheDisplayTitleIsNotDuplicated() throws IOException {
        manifestWithOnePage();
        responses.put("/api/v1/se/films/2026/films/manifest",
                "{\"pages\":[{\"url\":\"" + baseUrl() + "/public/api/se/films/2026/films.json\"}]}");
        responses.put("/public/api/se/films/2026/films.json", "{\"films\":["
                + "{\"id\":\"14307-film\",\"international_title\":\"Full Phil\",\"original_title\":\"Full Phil\"}]}");
        responses.put("/public/api/se/films/2026/sessions.json", "{\"sessions\":[{"
                + "\"name\":{\"es\":\"Full Phil\"},\"start_date\":\"2026-10-09T19:00:00\","
                + "\"locations\":[],\"films\":[\"14307-film\"]}]}");

        List<FilmScreening> screenings = client().screenings();

        assertThat(screenings).singleElement().satisfies(s -> assertThat(s.originalTitle()).isNull());
    }

    @Test
    void resolvesTheFilmsOwnPageUrlFromTheCatalogue() throws IOException {
        manifestWithOnePage();
        responses.put("/api/v1/se/films/2026/films/manifest",
                "{\"pages\":[{\"url\":\"" + baseUrl() + "/public/api/se/films/2026/films.json\"}]}");
        responses.put("/public/api/se/films/2026/films.json", "{\"films\":["
                + "{\"id\":\"14307-film\",\"international_title\":\"Full Phil\","
                + "\"url\":{\"es\":\"/es/film/2026/full-phil\"}}]}");
        responses.put("/public/api/se/films/2026/sessions.json", "{\"sessions\":[{"
                + "\"name\":{\"es\":\"Full Phil\"},\"start_date\":\"2026-10-09T19:00:00\","
                + "\"locations\":[],\"films\":[\"14307-film\"]}]}");

        List<FilmScreening> screenings = client().screenings();

        assertThat(screenings).singleElement().satisfies(s ->
                assertThat(s.pageUrl()).isEqualTo(baseUrl() + "/es/film/2026/full-phil"));
    }

    @Test
    void aFilmResolvedOnlyThroughTheSessionsOwnNameHasNoPageUrl() throws IOException {
        manifestWithOnePage();
        responses.put("/public/api/se/films/2026/sessions.json", "{\"sessions\":[{"
                + "\"name\":{\"es\":\"Buddy\"},\"start_date\":\"2026-10-09T19:00:00\",\"locations\":[]}]}");

        List<FilmScreening> screenings = client().screenings();

        assertThat(screenings).singleElement().satisfies(s -> assertThat(s.pageUrl()).isNull());
    }

    @Test
    void aFailingVenueLookupLeavesLocationNull() throws IOException {
        statusCodes.put("/public/api/films/locations.json", 500);
        manifestWithOnePage();
        responses.put("/public/api/se/films/2026/sessions.json", "{\"sessions\":[{"
                + "\"name\":{\"es\":\"Buddy\"},\"start_date\":\"2026-10-09T19:00:00\","
                + "\"locations\":[\"392-location\"]}]}");

        List<FilmScreening> screenings = client().screenings();

        assertThat(screenings).singleElement().satisfies(s -> assertThat(s.location()).isNull());
    }
}
