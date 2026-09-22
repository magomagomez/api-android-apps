package com.magomez.androidapps.movierec.provider.sitges;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.movierec.model.FilmScreening;
import com.magomez.androidapps.movierec.support.ExternalCallExecutor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ScheduleSource} backed by the JSON API the festival's own website uses to render
 * its programme page (public, unauthenticated, the same one the site itself calls — not
 * scraping rendered HTML). The page itself loads its data via JavaScript, so the listing
 * is not present in the raw HTML; this calls the underlying manifest/pages API directly.
 *
 * <p>Sessions are paginated ({@code sessions/manifest} &rarr; a list of page URLs), fetched
 * in parallel via {@link ExternalCallExecutor}. Locations are a single small unpaginated
 * call, fetched once and used to resolve each session's venue name.
 *
 * <p>A page that fails to load is logged and skipped (partial results beat none); the
 * locations lookup failing is likewise non-fatal (screenings still come back with no
 * location name).
 */
@Component
public class SitgesScheduleClient implements ScheduleSource {

    private static final String DEFAULT_BASE_URL = "https://sitgesfilmfestival.com";
    private static final String DEFAULT_USER_AGENT =
            "movie-recommendation-engine/1.0 (https://github.com/Magomez; personal, non-commercial)";
    private static final Logger log = LoggerFactory.getLogger(SitgesScheduleClient.class);

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(10))
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final String baseUrl;
    private final int edition;
    private final String userAgent;
    private final ExternalCallExecutor externalCallExecutor;

    @Autowired
    public SitgesScheduleClient(
            @Value("${movierec.sitges.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${movierec.sitges.edition:2026}") int edition,
            @Value("${movierec.sitges.user-agent:" + DEFAULT_USER_AGENT + "}") String userAgent,
            ExternalCallExecutor externalCallExecutor) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.edition = edition;
        this.userAgent = userAgent == null || userAgent.isBlank() ? DEFAULT_USER_AGENT : userAgent;
        this.externalCallExecutor = externalCallExecutor;
    }

    /** Test constructor: fetches sequentially, on the caller thread. */
    public SitgesScheduleClient(String baseUrl, int edition, String userAgent) {
        this(baseUrl, edition, userAgent, ExternalCallExecutor.sequential());
    }

    @Override
    public List<FilmScreening> screenings() throws IOException {
        Map<String, String> locationNames = fetchLocationNames();

        ManifestResponse manifest = getJson(
                "/api/v1/se/films/" + edition + "/sessions/manifest?format=full", ManifestResponse.class);
        List<String> pageUrls = manifest.pages() == null ? List.of()
                : manifest.pages().stream().map(ManifestPage::url).toList();

        List<List<FilmScreening>> perPage = externalCallExecutor.map(pageUrls,
                url -> fetchSessionsPage(url, locationNames));

        List<FilmScreening> all = new ArrayList<>();
        perPage.forEach(all::addAll);
        return all;
    }

    private List<FilmScreening> fetchSessionsPage(String pageUrl, Map<String, String> locationNames) {
        try {
            SessionsPageResponse page = getJson(pageUrl, SessionsPageResponse.class);
            if (page.sessions() == null) {
                return List.of();
            }
            List<FilmScreening> screenings = new ArrayList<>();
            for (SessionDto session : page.sessions()) {
                toScreening(session, locationNames).ifPresent(screenings::add);
            }
            return screenings;
        } catch (IOException e) {
            log.warn("Could not load Sitges sessions page {}: {}", pageUrl, e.getMessage());
            return List.of();
        }
    }

    private static java.util.Optional<FilmScreening> toScreening(SessionDto session, Map<String, String> locationNames) {
        String title = session.name() == null ? null : session.name().get("es");
        if (title == null || title.isBlank() || session.startDate() == null) {
            return java.util.Optional.empty();
        }
        LocalDateTime start = LocalDateTime.parse(session.startDate());
        LocalDateTime end = session.endDate() == null ? null : LocalDateTime.parse(session.endDate());
        String location = (session.locations() == null || session.locations().isEmpty())
                ? null : locationNames.get(session.locations().get(0));
        return java.util.Optional.of(new FilmScreening(
                title.trim(), start.toLocalDate(), start.toLocalTime(),
                end == null ? null : end.toLocalTime(), location));
    }

    private Map<String, String> fetchLocationNames() {
        Map<String, String> names = new ConcurrentHashMap<>();
        try {
            LocationsResponse response = getJson("/public/api/films/locations.json", LocationsResponse.class);
            if (response.locations() != null) {
                for (LocationDto location : response.locations()) {
                    String name = location.name() == null ? null : location.name().get("es");
                    if (location.id() != null && name != null) {
                        names.put(location.id(), name);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not load Sitges venue names: {}", e.getMessage());
        }
        return names;
    }

    private <T> T getJson(String path, Class<T> type) throws IOException {
        String url = path.startsWith("http") ? path : baseUrl + path;
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null) {
            throw new IOException("Invalid Sitges API URL: " + url);
        }
        Request request = new Request.Builder().url(parsed)
                .header("User-Agent", userAgent)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("Sitges API request failed with HTTP " + response.code() + " for " + url);
            }
            return objectMapper.readValue(body.string(), type);
        }
    }

    private static String stripTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ManifestResponse(List<ManifestPage> pages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ManifestPage(String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SessionsPageResponse(List<SessionDto> sessions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SessionDto(
            Map<String, String> name,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("end_date") String endDate,
            List<String> locations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LocationsResponse(List<LocationDto> locations) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LocationDto(String id, Map<String, String> name) {
    }
}
