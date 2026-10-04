package com.magomez.androidapps.jctravels.parks;

import com.magomez.androidapps.jctravels.parks.dto.ParkHoursDTO;
import com.magomez.androidapps.jctravels.parks.dto.ScheduleEntry;
import com.magomez.androidapps.jctravels.parks.service.ParkHours;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Which opening hours the park header shows, from a themeparks.wiki /schedule. */
class ParkHoursTest {

    private static final List<ScheduleEntry> SCHEDULE = List.of(
            new ScheduleEntry("2026-10-04", "TICKETED_EVENT", "Early Entry", "2026-10-04T07:30:00-04:00", "2026-10-04T08:00:00-04:00"),
            new ScheduleEntry("2026-10-04", "OPERATING", null, "2026-10-04T08:00:00-04:00", "2026-10-04T20:00:00-04:00"),
            new ScheduleEntry("2026-10-05", "OPERATING", null, "2026-10-05T09:00:00-04:00", "2026-10-05T19:00:00-04:00"),
            new ScheduleEntry("2026-10-05", "TICKETED_EVENT", "Special Ticketed Event", "2026-10-05T19:30:00-04:00", "2026-10-06T00:00:00-04:00"));

    @Test
    void beforeOpeningItIsTodayWithEarlyEntry() {
        ParkHoursDTO hours = ParkHours.next(SCHEDULE, Instant.parse("2026-10-04T07:15:00Z"));

        assertThat(hours.date()).isEqualTo("2026-10-04");
        assertThat(hours.opening()).isEqualTo("2026-10-04T08:00:00-04:00");
        assertThat(hours.closing()).isEqualTo("2026-10-04T20:00:00-04:00");
        assertThat(hours.earlyEntry()).isEqualTo("2026-10-04T07:30:00-04:00");
    }

    @Test
    void whileOpenItIsStillToday() {
        assertThat(ParkHours.next(SCHEDULE, Instant.parse("2026-10-04T23:59:00Z")).date()).isEqualTo("2026-10-04");
    }

    @Test
    void afterClosingItIsTheNextDayWithoutOtherEvents() {
        ParkHoursDTO hours = ParkHours.next(SCHEDULE, Instant.parse("2026-10-05T00:30:00Z"));

        assertThat(hours.date()).isEqualTo("2026-10-05");
        assertThat(hours.opening()).isEqualTo("2026-10-05T09:00:00-04:00");
        assertThat(hours.earlyEntry()).isNull();
    }

    @Test
    void noHoursLeftMeansNothingToShow() {
        assertThat(ParkHours.next(SCHEDULE, Instant.parse("2026-10-07T00:00:00Z"))).isNull();
        assertThat(ParkHours.next(List.of(), Instant.parse("2026-10-04T12:00:00Z"))).isNull();
    }
}
