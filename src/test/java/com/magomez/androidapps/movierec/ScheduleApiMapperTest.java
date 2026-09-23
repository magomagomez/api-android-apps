package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.api.ScheduleApiMapper;
import com.magomez.androidapps.movierec.api.ScheduleRequest;
import com.magomez.androidapps.movierec.api.ScheduleResponse;
import com.magomez.androidapps.movierec.schedule.ScheduleDay;
import com.magomez.androidapps.movierec.schedule.ScheduleResult;
import com.magomez.androidapps.movierec.schedule.ScheduledSession;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@link ScheduleApiMapper}: request validation and the domain-to-JSON shape. */
class ScheduleApiMapperTest {

    @Test
    void aBlankTitleIsRejected() {
        assertThatThrownBy(() -> ScheduleApiMapper.toTitles(new ScheduleRequest(Arrays.asList("Buddy", " "))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("titles[1]");
    }

    @Test
    void anEmptyRequestIsRejected() {
        assertThatThrownBy(() -> ScheduleApiMapper.toTitles(new ScheduleRequest(List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void aNullRequestIsTreatedAsEmptyAndRejected() {
        assertThatThrownBy(() -> ScheduleApiMapper.toTitles(null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void mapsDaysWeekdayNamesAndTheHolidayFlag() {
        ScheduledSession session = new ScheduledSession(
                "Buddy", LocalTime.of(20, 0), LocalTime.of(21, 41), "Auditori Meliá", true, List.of("Buddy"));
        ScheduleResult result = new ScheduleResult(
                List.of("Buddy"), List.of(),
                List.of(new ScheduleDay(LocalDate.of(2026, 10, 12), List.of(session))));

        ScheduleResponse response = ScheduleApiMapper.toResponse(result);

        assertThat(response.days()).hasSize(1);
        ScheduleResponse.ScheduleDayView day = response.days().get(0);
        assertThat(day.date()).isEqualTo("2026-10-12");
        assertThat(day.weekday()).isEqualTo("Lunes");
        assertThat(day.holiday()).isTrue();
        ScheduleResponse.SessionView sessionView = day.sessions().get(0);
        assertThat(sessionView.title()).isEqualTo("Buddy");
        assertThat(sessionView.startTime()).isEqualTo("20:00");
        assertThat(sessionView.endTime()).isEqualTo("21:41");
        assertThat(sessionView.location()).isEqualTo("Auditori Meliá");
        assertThat(sessionView.convenient()).isTrue();
        assertThat(sessionView.doubleBill()).isFalse();
        assertThat(sessionView.sessionFilms()).containsExactly("Buddy");
    }

    @Test
    void flagsADoubleBillAndListsEveryFilmInTheSession() {
        ScheduledSession session = new ScheduledSession("Full Phil", LocalTime.of(23, 15), LocalTime.of(1, 41),
                "Tramuntana", true, List.of("Full Phil", "Vertiginous"));
        ScheduleResult result = new ScheduleResult(List.of("Full Phil"), List.of(),
                List.of(new ScheduleDay(LocalDate.of(2026, 10, 13), List.of(session))));

        ScheduleResponse.SessionView sessionView = ScheduleApiMapper.toResponse(result).days().get(0).sessions().get(0);

        assertThat(sessionView.doubleBill()).isTrue();
        assertThat(sessionView.sessionFilms()).containsExactly("Full Phil", "Vertiginous");
    }

    @Test
    void aRegularDayIsNotFlaggedAsTheHoliday() {
        ScheduleResult result = new ScheduleResult(List.of("Buddy"), List.of(),
                List.of(new ScheduleDay(LocalDate.of(2026, 10, 9), List.of())));

        assertThat(ScheduleApiMapper.toResponse(result).days().get(0).holiday()).isFalse();
    }

    @Test
    void carriesTheNotScheduledTitlesThrough() {
        ScheduleResult result = new ScheduleResult(List.of("Buddy", "Ghost Film"), List.of("Ghost Film"), List.of());

        assertThat(ScheduleApiMapper.toResponse(result).notScheduled()).containsExactly("Ghost Film");
    }
}
