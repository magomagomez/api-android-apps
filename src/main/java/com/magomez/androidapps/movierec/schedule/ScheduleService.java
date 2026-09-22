package com.magomez.androidapps.movierec.schedule;

import com.magomez.androidapps.movierec.model.FilmScreening;
import com.magomez.androidapps.movierec.provider.festival.FestivalLineup;
import com.magomez.androidapps.movierec.provider.sitges.ScheduleSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns a flat list of requested titles into a day-by-day calendar of when they actually
 * screen at the festival — so picking a personal TOP N translates into "which days do I
 * need to be in Sitges, and at what time".
 *
 * <p>The full programme is fetched once per process and reused ({@link #loadScreenings()}
 * — same pattern as {@code FestivalLineupAccoladeProvider}: the schedule doesn't change
 * during a run, and re-fetching ~500 sessions per request would be wasted latency). Titles
 * are matched the same accent/punctuation-insensitive way festival lineups are
 * ({@link FestivalLineup#normalizeTitle(String)}) — a title with no match is reported in
 * {@link ScheduleResult#notScheduled()}, never silently dropped.
 */
@Service
public class ScheduleService {

    private final ScheduleSource scheduleSource;

    private volatile List<FilmScreening> screenings;

    public ScheduleService(ScheduleSource scheduleSource) {
        this.scheduleSource = scheduleSource;
    }

    /** Non-blocking: {@code true} once the programme is cached and a request won't fetch it first. */
    public boolean isReady() {
        return screenings != null;
    }

    /** Triggers the (cached, once-per-process) programme fetch eagerly, for startup warm-up. */
    public void warmUp() throws IOException {
        loadScreenings();
    }

    public ScheduleResult buildSchedule(List<String> requestedTitles) throws IOException {
        Objects.requireNonNull(requestedTitles, "requestedTitles");

        Map<String, List<FilmScreening>> byNormalizedTitle = new LinkedHashMap<>();
        for (FilmScreening screening : loadScreenings()) {
            byNormalizedTitle
                    .computeIfAbsent(FestivalLineup.normalizeTitle(screening.title()), k -> new ArrayList<>())
                    .add(screening);
        }

        List<String> notScheduled = new ArrayList<>();
        Map<java.time.LocalDate, List<ScheduledSession>> sessionsByDate = new LinkedHashMap<>();

        for (String requestedTitle : requestedTitles) {
            List<FilmScreening> matches = byNormalizedTitle.get(FestivalLineup.normalizeTitle(requestedTitle));
            if (matches == null || matches.isEmpty()) {
                notScheduled.add(requestedTitle);
                continue;
            }
            for (FilmScreening screening : matches) {
                boolean convenient = SchedulePriority.isConvenient(screening.date(), screening.startTime());
                ScheduledSession session = new ScheduledSession(
                        requestedTitle, screening.startTime(), screening.endTime(), screening.location(), convenient);
                sessionsByDate.computeIfAbsent(screening.date(), k -> new ArrayList<>()).add(session);
            }
        }

        List<ScheduleDay> days = sessionsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ScheduleDay(e.getKey(), e.getValue().stream()
                        .sorted(Comparator.comparing(ScheduledSession::startTime))
                        .toList()))
                .toList();

        return new ScheduleResult(requestedTitles, notScheduled, days);
    }

    private List<FilmScreening> loadScreenings() throws IOException {
        List<FilmScreening> result = screenings;
        if (result == null) {
            synchronized (this) {
                result = screenings;
                if (result == null) {
                    result = scheduleSource.screenings();
                    screenings = result;
                }
            }
        }
        return result;
    }
}
