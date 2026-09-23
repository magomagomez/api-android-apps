package com.magomez.androidapps.movierec.api;

import com.magomez.androidapps.movierec.schedule.ScheduleDay;
import com.magomez.androidapps.movierec.schedule.ScheduleResult;
import com.magomez.androidapps.movierec.schedule.ScheduledSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Translates between the HTTP contract ({@link ScheduleRequest} / {@link ScheduleResponse})
 * and the domain ({@link ScheduleResult}). Pure and stateless, like {@link RecommendationApiMapper}.
 */
public final class ScheduleApiMapper {

    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private ScheduleApiMapper() {
    }

    /** Validates the request and converts it to a plain list of requested titles. */
    public static List<String> toTitles(ScheduleRequest request) {
        List<String> titles = request == null ? List.of() : request.titles();
        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i);
            if (title == null || title.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titles[" + i + "] is required");
            }
        }
        if (titles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titles must not be empty");
        }
        return titles;
    }

    public static ScheduleResponse toResponse(ScheduleResult result) {
        List<ScheduleResponse.ScheduleDayView> days = result.days().stream()
                .map(ScheduleApiMapper::toDayView)
                .toList();
        return new ScheduleResponse(result.notScheduled(), days);
    }

    private static ScheduleResponse.ScheduleDayView toDayView(ScheduleDay day) {
        String weekdayName = day.date().getDayOfWeek().getDisplayName(TextStyle.FULL, SPANISH);
        String weekday = capitalize(weekdayName);
        List<ScheduleResponse.SessionView> sessions = day.sessions().stream()
                .map(ScheduleApiMapper::toSessionView)
                .toList();
        return new ScheduleResponse.ScheduleDayView(
                day.date().toString(), weekday, day.date().getDayOfMonth() == 12, sessions);
    }

    private static ScheduleResponse.SessionView toSessionView(ScheduledSession session) {
        return new ScheduleResponse.SessionView(
                session.title(),
                session.startTime().format(TIME),
                session.endTime() == null ? null : session.endTime().format(TIME),
                session.location(),
                session.convenient(),
                session.sessionFilms().size() > 1,
                session.sessionFilms());
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
