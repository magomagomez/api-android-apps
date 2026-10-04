package com.magomez.androidapps.jctravels.rides.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.jctravels.rides.dto.ParkFanAttraction;
import com.magomez.androidapps.jctravels.rides.dto.ParkFanSearch;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * park.fan, used only for the wait forecast. Which park.fan attraction is each ride is kept
 * for a day (misses too, so an unknown ride does not search on every visit); forecasts for
 * 5 minutes, which is how often park.fan refreshes them.
 */
@Service
public class ParkFanService {

    private static final String BASE_URL = "https://api.park.fan";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration KEEP_MATCH_FOR = Duration.ofDays(1);
    private static final Duration KEEP_FORECAST_FOR = Duration.ofMinutes(5);

    private final OkHttpClient client = new OkHttpClient.Builder().callTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final Map<String, Cached<Optional<String>>> matches = new ConcurrentHashMap<>();
    private final Map<String, Cached<ParkFanAttraction>> forecasts = new ConcurrentHashMap<>();

    private record Cached<T>(Instant until, T value) {
        boolean fresh() {
            return until.isAfter(Instant.now());
        }
    }

    /** The park.fan path of the attraction with this official name in this park, or null if there is none. */
    public String attractionUrl(String rideName, String parkName) throws IOException {
        String key = parkName + "|" + rideName;
        Cached<Optional<String>> cached = matches.get(key);
        if (cached != null && cached.fresh()) {
            return cached.value().orElse(null);
        }
        HttpUrl url = HttpUrl.get(BASE_URL + "/v1/search").newBuilder().addQueryParameter("q", rideName).build();
        String found = RideForecasts.match(get(url, ParkFanSearch.class), rideName, parkName);
        matches.put(key, new Cached<>(Instant.now().plus(KEEP_MATCH_FOR), Optional.ofNullable(found)));
        return found;
    }

    public ParkFanAttraction attraction(String path) throws IOException {
        Cached<ParkFanAttraction> cached = forecasts.get(path);
        if (cached != null && cached.fresh()) {
            return cached.value();
        }
        ParkFanAttraction attraction = get(HttpUrl.get(BASE_URL + path), ParkFanAttraction.class);
        forecasts.put(path, new Cached<>(Instant.now().plus(KEEP_FORECAST_FOR), attraction));
        return attraction;
    }

    private <T> T get(HttpUrl url, Class<T> type) throws IOException {
        try (Response response = client.newCall(new Request.Builder().url(url).build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("park.fan answered HTTP " + response.code());
            }
            return objectMapper.readValue(response.body().string(), type);
        }
    }
}
