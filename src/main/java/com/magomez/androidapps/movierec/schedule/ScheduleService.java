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
 * during a run, and re-fetching ~500 sessions per request would be wasted latency). A
 * screening is indexed under both its display title and its
 * {@link FilmScreening#originalTitle()} when the catalogue gives a different one (e.g. a
 * requester may know a film by its native-language name, not the one it's marketed under
 * internationally). Titles are matched the same accent/punctuation-insensitive way festival
 * lineups are ({@link FestivalLineup#normalizeTitle(String)}); when that isn't an exact hit,
 * a word-sequence prefix match is tried too (e.g. a requested {@code "Cold War"} still finds
 * the programme's {@code "Cold War 1994"} — a same-word-order year/subtitle suffix, not a
 * different film with a similar name). A title with no match either way is reported in
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

    /**
     * Every screening of {@code title} (matched the same accent/prefix-tolerant way
     * {@link #buildSchedule} does), in source order. Empty when nothing matches — never
     * guessed. For plugging schedule facts onto a single film (e.g. a recommendation),
     * not the whole calendar.
     */
    public List<FilmScreening> screeningsOf(String title) throws IOException {
        Objects.requireNonNull(title, "title");
        List<FilmScreening> matches = findMatches(title, indexByNormalizedTitle());
        return matches == null ? List.of() : matches;
    }

    public ScheduleResult buildSchedule(List<String> requestedTitles) throws IOException {
        Objects.requireNonNull(requestedTitles, "requestedTitles");

        Map<String, List<FilmScreening>> byNormalizedTitle = indexByNormalizedTitle();

        List<String> notScheduled = new ArrayList<>();
        Map<java.time.LocalDate, List<ScheduledSession>> sessionsByDate = new LinkedHashMap<>();

        for (String requestedTitle : requestedTitles) {
            List<FilmScreening> matches = findMatches(requestedTitle, byNormalizedTitle);
            if (matches == null || matches.isEmpty()) {
                notScheduled.add(requestedTitle);
                continue;
            }
            for (FilmScreening screening : matches) {
                boolean convenient = SchedulePriority.isConvenient(screening.date(), screening.startTime());
                ScheduledSession session = new ScheduledSession(requestedTitle, screening.startTime(),
                        screening.endTime(), screening.location(), convenient, screening.sessionFilms());
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

    private Map<String, List<FilmScreening>> indexByNormalizedTitle() throws IOException {
        Map<String, List<FilmScreening>> byNormalizedTitle = new LinkedHashMap<>();
        for (FilmScreening screening : loadScreenings()) {
            index(byNormalizedTitle, screening.title(), screening);
            if (screening.originalTitle() != null) {
                index(byNormalizedTitle, screening.originalTitle(), screening);
            }
        }
        return byNormalizedTitle;
    }

    private static void index(Map<String, List<FilmScreening>> byNormalizedTitle, String title, FilmScreening screening) {
        byNormalizedTitle.computeIfAbsent(FestivalLineup.normalizeTitle(title), k -> new ArrayList<>()).add(screening);
    }

    private static List<FilmScreening> findMatches(String requestedTitle, Map<String, List<FilmScreening>> byNormalizedTitle) {
        String normalizedRequest = FestivalLineup.normalizeTitle(requestedTitle);
        List<FilmScreening> exact = byNormalizedTitle.get(normalizedRequest);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, List<FilmScreening>> entry : byNormalizedTitle.entrySet()) {
            if (isWordSequencePrefix(normalizedRequest, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * {@code true} when one normalized title's words are, in order, a prefix of the
     * other's — e.g. {@code "cold war"} is a prefix of {@code "cold war 1994"}. Deliberately
     * word-based (not a raw substring check) so {@code "cold"} alone can never match
     * {@code "coldwar"} or similar unrelated titles.
     */
    private static boolean isWordSequencePrefix(String normalizedA, String normalizedB) {
        if (normalizedA.isEmpty() || normalizedB.isEmpty()) {
            return false;
        }
        List<String> wordsA = List.of(normalizedA.split(" "));
        List<String> wordsB = List.of(normalizedB.split(" "));
        List<String> shorter = wordsA.size() <= wordsB.size() ? wordsA : wordsB;
        List<String> longer = wordsA.size() <= wordsB.size() ? wordsB : wordsA;
        return !shorter.equals(longer) && longer.subList(0, shorter.size()).equals(shorter);
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
