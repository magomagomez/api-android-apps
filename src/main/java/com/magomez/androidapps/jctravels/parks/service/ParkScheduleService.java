package com.magomez.androidapps.jctravels.parks.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magomez.androidapps.jctravels.parks.dto.ParkSchedule;
import com.magomez.androidapps.jctravels.parks.dto.ScheduleEntry;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A park's opening calendar from themeparks.wiki. Schedules change rarely, so each one is
 * kept in memory for a while instead of being fetched on every parks request.
 */
@Service
public class ParkScheduleService {

    private static final String SCHEDULE_URL = "https://api.themeparks.wiki/v1/entity/%s/schedule";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration KEEP_FOR = Duration.ofMinutes(30);

    private final OkHttpClient client = new OkHttpClient.Builder().callTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(Instant fetchedAt, List<ScheduleEntry> schedule) {
    }

    public List<ScheduleEntry> schedule(String queueId) throws IOException {
        Cached cached = cache.get(queueId);
        if (cached != null && cached.fetchedAt().plus(KEEP_FOR).isAfter(Instant.now())) {
            return cached.schedule();
        }
        List<ScheduleEntry> schedule = fetch(queueId);
        cache.put(queueId, new Cached(Instant.now(), schedule));
        return schedule;
    }

    private List<ScheduleEntry> fetch(String queueId) throws IOException {
        Request request = new Request.Builder().url(String.format(SCHEDULE_URL, queueId)).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Schedule request failed with HTTP " + response.code());
            }
            ParkSchedule body = objectMapper.readValue(response.body().string(), ParkSchedule.class);
            return body.schedule() == null ? List.of() : body.schedule();
        }
    }
}
