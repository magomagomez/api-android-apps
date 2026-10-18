package com.magomez.androidapps.jcfit.training;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static com.magomez.androidapps.jcfit.training.TrainingFixtures.assertApiError;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.utcClock;
import static org.assertj.core.api.Assertions.assertThat;

/** Week numbers counted from the program start and "today" in Spain. */
class TrainingDatesTest {

    private static final LocalDate MONDAY_START = LocalDate.of(2026, 9, 7);
    private static final LocalDate WEDNESDAY_START = LocalDate.of(2026, 9, 2);

    @Test
    void theWeekOfTheStartIsWeekOne() {
        assertThat(TrainingDates.weekNumber(MONDAY_START, MONDAY_START)).isEqualTo(1);
        assertThat(TrainingDates.weekNumber(MONDAY_START, LocalDate.of(2026, 9, 13))).isEqualTo(1);
        assertThat(TrainingDates.weekNumber(MONDAY_START, LocalDate.of(2026, 9, 14))).isEqualTo(2);
        assertThat(TrainingDates.weekNumber(MONDAY_START, LocalDate.of(2026, 10, 18))).isEqualTo(6);
    }

    @Test
    void aStartInMidweekMakesItsWholeMondayToSundayWeekOne() {
        assertThat(TrainingDates.weekNumber(WEDNESDAY_START, LocalDate.of(2026, 8, 31))).isEqualTo(1);
        assertThat(TrainingDates.weekNumber(WEDNESDAY_START, LocalDate.of(2026, 9, 6))).isEqualTo(1);
        assertThat(TrainingDates.weekNumber(WEDNESDAY_START, LocalDate.of(2026, 9, 7))).isEqualTo(2);
    }

    @Test
    void aWeekBeforeTheStartIsRejected() {
        assertApiError(() -> TrainingDates.weekNumber(WEDNESDAY_START, LocalDate.of(2026, 8, 30)),
                HttpStatus.BAD_REQUEST, "before_program_start");
    }

    @Test
    void mondayIsTheFirstDayOfTheWeek() {
        assertThat(TrainingDates.monday(LocalDate.of(2026, 10, 18))).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(TrainingDates.monday(LocalDate.of(2026, 10, 12))).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void todayIsTheDayInMadridWhateverTheClockZone() {
        // 22:30 UTC on Thursday is already 00:30 on Friday in Madrid (CEST, UTC+2).
        assertThat(TrainingDates.today(utcClock("2026-10-15T22:30:00Z"))).isEqualTo(LocalDate.of(2026, 10, 16));
        assertThat(TrainingDates.today(utcClock("2026-10-15T21:59:00Z"))).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void aDateOnAnotherWeekdayIsRejected() {
        TrainingDates.requireDayOfWeek(3, LocalDate.of(2026, 10, 14));

        assertApiError(() -> TrainingDates.requireDayOfWeek(4, LocalDate.of(2026, 10, 14)),
                HttpStatus.BAD_REQUEST, "bad_request");
    }
}
