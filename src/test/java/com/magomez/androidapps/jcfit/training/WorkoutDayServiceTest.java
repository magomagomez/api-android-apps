package com.magomez.androidapps.jcfit.training;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static com.magomez.androidapps.jcfit.training.TrainingFixtures.ACTIVE_DAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.DAY_EXERCISES;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.LAST_DATE;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.PREVIOUS_SETS;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.SESSION_OF_DAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.SESSION_SETS;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.WEDNESDAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.activeDay;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.assertApiError;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.exercise;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.session;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.set;
import static org.assertj.core.api.Assertions.assertThat;

/** GET /days/{programDayId}?date=: a whole day in a fixed number of queries, whatever its exercises. */
class WorkoutDayServiceTest {

    private static final int DAY_ID = 5;
    private static final LocalDate LAST_WEEK = WEDNESDAY.minusDays(7);

    private RecordingJdbcTemplate jdbc;
    private WorkoutDayService service;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate()
                .rows(ACTIVE_DAY, activeDay(DAY_ID, 3, "Pierna"))
                .rows(DAY_EXERCISES, exercise(11, DAY_ID, 4, 1, 3), exercise(12, DAY_ID, 7, 2, 3),
                        exercise(13, DAY_ID, 9, 3, 3))
                .rows(SESSION_OF_DAY, session(44, DAY_ID, WEDNESDAY, null))
                .rows(SESSION_SETS, set(44, WEDNESDAY, 4, 1, "62.5", 10, true), set(44, WEDNESDAY, 7, 1, null, null, false))
                .rows(PREVIOUS_SETS, set(37, LAST_WEEK, 4, 1, "60", 10, true))
                .scalar(LAST_DATE, LAST_WEEK);
        service = new WorkoutDayService(new TrainingProgramDao(jdbc), new WorkoutSessionDao(jdbc), new SetLogDao(jdbc));
    }

    @Test
    void theDayIsReadInSixQueriesWithItsParameters() {
        WorkoutDayView view = service.day(DAY_ID, WEDNESDAY);

        assertThat(view.sessionId()).isEqualTo(44);
        assertThat(view.startedAt()).isEqualTo(Instant.parse("2026-10-14T17:40:00Z"));
        assertThat(view.lastDate()).isEqualTo(LAST_WEEK);
        assertThat(view.exercises()).hasSize(3);
        assertThat(view.exercises().get(0).logged()).containsExactly(new LoggedSet(1, new BigDecimal("62.5"), 10, true));
        assertThat(view.exercises().get(0).previous().date()).isEqualTo(LAST_WEEK);
        assertThat(view.exercises().get(1).logged()).containsExactly(new LoggedSet(1, null, null, false));
        assertThat(view.exercises().get(2).logged()).isEmpty();

        assertThat(jdbc.calls).hasSize(6);
        assertThat(jdbc.callsWith(ACTIVE_DAY).get(0).args()).containsExactly(DAY_ID);
        assertThat(jdbc.callsWith(SESSION_OF_DAY).get(0).args()).containsExactly(DAY_ID, WEDNESDAY);
        assertThat(jdbc.callsWith(SESSION_SETS).get(0).args()).containsExactly(44);
        assertThat(jdbc.callsWith(PREVIOUS_SETS).get(0).args()).containsExactly(WEDNESDAY, DAY_ID);
        assertThat(jdbc.callsWith(LAST_DATE).get(0).args()).containsExactly(DAY_ID, WEDNESDAY);
    }

    @Test
    void aDayNotStartedDoesNotReadLoggedSets() {
        jdbc.rows(SESSION_OF_DAY);

        WorkoutDayView view = service.day(DAY_ID, WEDNESDAY);

        assertThat(view.sessionId()).isNull();
        assertThat(jdbc.callsWith(SESSION_SETS)).isEmpty();
        assertThat(view.exercises()).allSatisfy(exercise -> assertThat(exercise.logged()).isEmpty());
    }

    @Test
    void anUnknownDayOrOneOutsideTheActiveProgramIsNotFound() {
        jdbc.rows(ACTIVE_DAY);

        assertApiError(() -> service.day(DAY_ID, WEDNESDAY), HttpStatus.NOT_FOUND, "not_found");
    }

    @Test
    void aDateOnAnotherWeekdayIsRejected() {
        assertApiError(() -> service.day(DAY_ID, WEDNESDAY.plusDays(1)), HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.callsWith(DAY_EXERCISES)).isEmpty();
    }

    @Test
    void aDateBeforeTheProgramIsRejected() {
        assertApiError(() -> service.day(DAY_ID, LocalDate.of(2026, 8, 26)), HttpStatus.BAD_REQUEST,
                "before_program_start");
    }
}
