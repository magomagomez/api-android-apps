package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.model.FilmScreening;
import com.magomez.androidapps.movierec.provider.sitges.ScheduleSource;
import com.magomez.androidapps.movierec.schedule.ScheduleDay;
import com.magomez.androidapps.movierec.schedule.ScheduleResult;
import com.magomez.androidapps.movierec.schedule.ScheduleService;
import com.magomez.androidapps.movierec.schedule.ScheduledSession;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ScheduleService}: matches requested titles against the festival's full programme
 * (accent/case-insensitively), groups by day, and never re-fetches the programme once cached.
 */
class ScheduleServiceTest {

    private static FilmScreening screening(String title, LocalDate date, LocalTime start, String location) {
        return new FilmScreening(title, date, start, start.plusHours(2), location);
    }

    @Test
    void matchesARequestedTitleAccentAndCaseInsensitively() throws IOException {
        ScheduleSource source = () -> List.of(
                screening("El Éxito", LocalDate.of(2026, 10, 9), LocalTime.of(20, 0), "Auditori"));
        ScheduleService service = new ScheduleService(source);

        ScheduleResult result = service.buildSchedule(List.of("el exito"));

        assertThat(result.notScheduled()).isEmpty();
        assertThat(result.days()).hasSize(1);
        assertThat(result.days().get(0).sessions()).hasSize(1);
        assertThat(result.days().get(0).sessions().get(0).title()).isEqualTo("el exito");
    }

    @Test
    void aTitleWithNoMatchingScreeningIsReportedAsNotScheduled() throws IOException {
        ScheduleSource source = () -> List.of();
        ScheduleService service = new ScheduleService(source);

        ScheduleResult result = service.buildSchedule(List.of("Nowhere Film"));

        assertThat(result.notScheduled()).containsExactly("Nowhere Film");
        assertThat(result.days()).isEmpty();
    }

    @Test
    void aFilmWithSeveralScreeningsAppearsOnEachOfItsDays() throws IOException {
        ScheduleSource source = () -> List.of(
                screening("Buddy", LocalDate.of(2026, 10, 9), LocalTime.of(19, 0), "Auditori"),
                screening("Buddy", LocalDate.of(2026, 10, 11), LocalTime.of(11, 0), "Retiro"));
        ScheduleService service = new ScheduleService(source);

        ScheduleResult result = service.buildSchedule(List.of("Buddy"));

        assertThat(result.days()).extracting(ScheduleDay::date)
                .containsExactly(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 11));
    }

    @Test
    void daysAreSortedAndSessionsWithinADayAreSortedByStartTime() throws IOException {
        ScheduleSource source = () -> List.of(
                screening("Late Show", LocalDate.of(2026, 10, 10), LocalTime.of(23, 0), null),
                screening("Early Show", LocalDate.of(2026, 10, 9), LocalTime.of(10, 0), null),
                screening("Also On The 10th", LocalDate.of(2026, 10, 10), LocalTime.of(9, 0), null));
        ScheduleService service = new ScheduleService(source);

        ScheduleResult result = service.buildSchedule(List.of("Late Show", "Early Show", "Also On The 10th"));

        assertThat(result.days()).extracting(ScheduleDay::date)
                .containsExactly(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10));
        List<ScheduledSession> tenth = result.days().get(1).sessions();
        assertThat(tenth).extracting(ScheduledSession::title).containsExactly("Also On The 10th", "Late Show");
    }

    @Test
    void theConvenientFlagFollowsSchedulePriority() throws IOException {
        ScheduleSource source = () -> List.of(
                screening("Weekday Morning", LocalDate.of(2026, 10, 14), LocalTime.of(10, 0), null), // Wed
                screening("Weekday Evening", LocalDate.of(2026, 10, 14), LocalTime.of(20, 0), null));
        ScheduleService service = new ScheduleService(source);

        ScheduleResult result = service.buildSchedule(List.of("Weekday Morning", "Weekday Evening"));

        List<ScheduledSession> sessions = result.days().get(0).sessions();
        assertThat(sessions).filteredOn(s -> s.title().equals("Weekday Morning"))
                .allMatch(s -> !s.convenient());
        assertThat(sessions).filteredOn(s -> s.title().equals("Weekday Evening"))
                .allMatch(ScheduledSession::convenient);
    }

    @Test
    void theProgrammeIsFetchedOnceAndReusedAcrossCalls() throws IOException {
        AtomicInteger fetches = new AtomicInteger();
        ScheduleSource source = () -> {
            fetches.incrementAndGet();
            return List.of(screening("Buddy", LocalDate.of(2026, 10, 9), LocalTime.of(19, 0), null));
        };
        ScheduleService service = new ScheduleService(source);

        service.buildSchedule(List.of("Buddy"));
        service.buildSchedule(List.of("Buddy"));

        assertThat(fetches).hasValue(1);
    }

    @Test
    void isReadyReflectsWhetherTheProgrammeHasBeenFetched() throws IOException {
        ScheduleSource source = () -> List.of();
        ScheduleService service = new ScheduleService(source);

        assertThat(service.isReady()).isFalse();
        service.warmUp();
        assertThat(service.isReady()).isTrue();
    }
}
