package com.magomez.androidapps.movierec.api;

import java.util.List;

/**
 * Body of {@code POST /api/schedule}.
 *
 * <pre>
 * { "titles": ["Full Phil", "Hope", "Buddy"] }
 * </pre>
 *
 * <p>These are titles already picked from a {@code /api/recommendations} response — this
 * endpoint only looks up when/where they screen, it doesn't score or identify anything.
 *
 * @param titles the requested titles, in the order the calendar should consider them
 */
public record ScheduleRequest(List<String> titles) {

    public ScheduleRequest {
        titles = titles == null ? List.of() : List.copyOf(titles);
    }
}
