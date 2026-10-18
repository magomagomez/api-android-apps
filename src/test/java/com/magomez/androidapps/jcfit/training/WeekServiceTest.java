package com.magomez.androidapps.jcfit.training;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.LocalDate;

import static com.magomez.androidapps.jcfit.training.TrainingFixtures.ACTIVE_PROGRAM;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.COMPLETED_COUNTS;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.DAYS_OF_PROGRAM;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.PROGRAM_EXERCISES;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.PROGRAM_START;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.SESSIONS_BETWEEN;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.WEDNESDAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.WEDNESDAY_NOON;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.assertApiError;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.count;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.day;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.exercise;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.program;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.session;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.utcClock;
import static org.assertj.core.api.Assertions.assertThat;

/** GET /weeks/{date}: reads the week in a fixed number of queries, with the parameters bound. */
class WeekServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 18);

    private RecordingJdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate()
                .rows(ACTIVE_PROGRAM, program(1, PROGRAM_START))
                .rows(DAYS_OF_PROGRAM, day(3, 1, "Empuje"), day(5, 3, "Pierna"))
                .rows(PROGRAM_EXERCISES, exercise(11, 3, 40, 1, 3), exercise(14, 5, 43, 1, 2))
                .rows(SESSIONS_BETWEEN, session(41, 3, MONDAY, "2026-10-12T18:00:00Z"), session(43, 5, WEDNESDAY, null))
                .rows(COMPLETED_COUNTS, count(41, 40, 3), count(43, 43, 1));
    }

    @Test
    void theWeekIsReadWithItsBoundsAsParameters() {
        WeekView week = service(WEDNESDAY_NOON).week(LocalDate.of(2026, 10, 16));

        assertThat(week.startDate()).isEqualTo(MONDAY);
        assertThat(week.days()).extracting(WeekDay::status).containsExactly(DayStatus.DONE, DayStatus.REST,
                DayStatus.TODAY, DayStatus.REST, DayStatus.REST, DayStatus.REST, DayStatus.REST);
        assertThat(week.days().get(0).completedExercises()).isEqualTo(1);
        assertThat(week.days().get(2).completedExercises()).isZero();
        assertThat(week.planned()).isEqualTo(2);
        assertThat(week.completed()).isEqualTo(1);
        assertThat(jdbc.callsWith(DAYS_OF_PROGRAM).get(0).args()).containsExactly(1);
        assertThat(jdbc.callsWith(PROGRAM_EXERCISES).get(0).args()).containsExactly(1);
        assertThat(jdbc.callsWith(SESSIONS_BETWEEN).get(0).args()).containsExactly(MONDAY, SUNDAY);
        assertThat(jdbc.callsWith(COMPLETED_COUNTS).get(0).args()).containsExactly(MONDAY, SUNDAY);
        assertThat(jdbc.calls).hasSize(5);
    }

    @Test
    void todayIsTheDayInMadridNearMidnightUtc() {
        // 22:30 UTC on Tuesday is 00:30 on Wednesday in Madrid.
        WeekView week = service(utcClock("2026-10-13T22:30:00Z")).week(MONDAY);

        assertThat(week.today()).isEqualTo(WEDNESDAY);
        assertThat(week.days().get(2).status()).isEqualTo(DayStatus.TODAY);
    }

    @Test
    void withoutAnActiveProgramIsNotFound() {
        jdbc.rows(ACTIVE_PROGRAM);

        assertApiError(() -> service(WEDNESDAY_NOON).week(WEDNESDAY), HttpStatus.NOT_FOUND, "no_active_program");
    }

    @Test
    void aWeekBeforeTheProgramIsRejectedBeforeReadingIt() {
        assertApiError(() -> service(WEDNESDAY_NOON).week(LocalDate.of(2026, 8, 30)), HttpStatus.BAD_REQUEST,
                "before_program_start");
        assertThat(jdbc.callsWith(SESSIONS_BETWEEN)).isEmpty();
    }

    private WeekService service(Clock clock) {
        return new WeekService(new TrainingProgramDao(jdbc), new WorkoutSessionDao(jdbc), new SetLogDao(jdbc), clock);
    }
}
