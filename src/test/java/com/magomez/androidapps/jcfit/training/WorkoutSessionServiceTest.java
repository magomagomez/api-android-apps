package com.magomez.androidapps.jcfit.training;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.magomez.androidapps.jcfit.training.TrainingFixtures.ACTIVE_DAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.DELETE_SET;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.FINISH;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.INSERT_SESSION;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.IS_IN_DAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.SESSION_BY_ID;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.SESSION_OF_DAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.UPSERT_SET;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.WEDNESDAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.WEDNESDAY_NOON;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.activeDay;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.assertApiError;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.session;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.utcClock;
import static org.assertj.core.api.Assertions.assertThat;

/** POST /sessions, PUT and DELETE of sets and POST /finish. */
class WorkoutSessionServiceTest {

    private static final int DAY_ID = 5;
    private static final int SESSION_ID = 44;
    private static final int SQUAT = 4;
    private static final OffsetDateTime NOON = OffsetDateTime.ofInstant(WEDNESDAY_NOON.instant(), ZoneOffset.UTC);

    private RecordingJdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate()
                .rows(ACTIVE_DAY, activeDay(DAY_ID, 3, "Pierna"))
                .rows(INSERT_SESSION, session(SESSION_ID, DAY_ID, WEDNESDAY, null))
                .rows(SESSION_BY_ID, session(SESSION_ID, DAY_ID, WEDNESDAY, null))
                .scalar(IS_IN_DAY, 1);
    }

    @Test
    void startingADayCreatesTheSessionWithTheDayNameAndTheClockTime() {
        StartedSession started = service(WEDNESDAY_NOON).start(new StartSessionRequest(DAY_ID, WEDNESDAY));

        assertThat(started.created()).isTrue();
        assertThat(started.session()).isEqualTo(new SessionReply(SESSION_ID, Instant.parse("2026-10-14T17:40:00Z"),
                null));
        RecordingJdbcTemplate.Call insert = jdbc.callsWith(INSERT_SESSION).get(0);
        assertThat(insert.sql()).contains("ON CONFLICT (program_day_id, session_date) DO NOTHING");
        assertThat(insert.args()).containsExactly(DAY_ID, "Pierna", WEDNESDAY, NOON);
        assertThat(jdbc.callsWith(SESSION_OF_DAY)).isEmpty();
    }

    @Test
    void startingADayAlreadyStartedReturnsTheSameSession() {
        jdbc.rows(INSERT_SESSION).rows(SESSION_OF_DAY, session(SESSION_ID, DAY_ID, WEDNESDAY, "2026-10-14T18:30:00Z"));

        StartedSession started = service(WEDNESDAY_NOON).start(new StartSessionRequest(DAY_ID, WEDNESDAY));

        assertThat(started.created()).isFalse();
        assertThat(started.session().sessionId()).isEqualTo(SESSION_ID);
        assertThat(started.session().finishedAt()).isEqualTo(Instant.parse("2026-10-14T18:30:00Z"));
        assertThat(jdbc.callsWith(SESSION_OF_DAY).get(0).args()).containsExactly(DAY_ID, WEDNESDAY);
    }

    @Test
    void aFutureDayCannotBeStarted() {
        assertApiError(() -> service(WEDNESDAY_NOON).start(new StartSessionRequest(DAY_ID, WEDNESDAY.plusDays(7))),
                HttpStatus.BAD_REQUEST, "future_date");
        assertThat(jdbc.callsWith(INSERT_SESSION)).isEmpty();
    }

    @Test
    void todayInMadridCanBeStartedWhileItIsStillYesterdayInUtc() {
        // 22:30 UTC on Tuesday is already Wednesday in Madrid.
        Clock tuesdayNightUtc = utcClock("2026-10-13T22:30:00Z");

        assertThat(service(tuesdayNightUtc).start(new StartSessionRequest(DAY_ID, WEDNESDAY)).created()).isTrue();
        assertApiError(() -> service(utcClock("2026-10-13T21:30:00Z")).start(new StartSessionRequest(DAY_ID, WEDNESDAY)),
                HttpStatus.BAD_REQUEST, "future_date");
    }

    @Test
    void startingNeedsAnActiveDayOnItsWeekday() {
        assertApiError(() -> service(WEDNESDAY_NOON).start(new StartSessionRequest(DAY_ID, WEDNESDAY.minusDays(1))),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service(WEDNESDAY_NOON).start(new StartSessionRequest(null, WEDNESDAY)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service(WEDNESDAY_NOON).start(null), HttpStatus.BAD_REQUEST, "bad_request");
        jdbc.rows(ACTIVE_DAY);
        assertApiError(() -> service(WEDNESDAY_NOON).start(new StartSessionRequest(DAY_ID, WEDNESDAY)),
                HttpStatus.NOT_FOUND, "not_found");
        assertThat(jdbc.callsWith(INSERT_SESSION)).isEmpty();
    }

    @Test
    void aSetIsUpsertedBySessionExerciseAndNumber() {
        SaveSetRequest request = new SaveSetRequest(SQUAT, 2, new BigDecimal("62.5"), 10, true);

        LoggedSet saved = service(WEDNESDAY_NOON).saveSet(SESSION_ID, request);
        service(WEDNESDAY_NOON).saveSet(SESSION_ID, request);

        assertThat(saved).isEqualTo(new LoggedSet(2, new BigDecimal("62.5"), 10, true));
        RecordingJdbcTemplate.Call upsert = jdbc.callsWith(UPSERT_SET).get(0);
        assertThat(upsert.sql()).contains("ON CONFLICT (session_id, exercise_id, set_number) DO UPDATE");
        assertThat(upsert.args()).containsExactly(SESSION_ID, SQUAT, 2, new BigDecimal("62.5"), 10, true, NOON);
        assertThat(jdbc.callsWith(UPSERT_SET)).hasSize(2);
        assertThat(jdbc.callsWith(IS_IN_DAY).get(0).args()).containsExactly(DAY_ID, SQUAT);
    }

    @Test
    void aSetAboveTheProgramAndOneOfAFinishedSessionAreAllowed() {
        jdbc.rows(SESSION_BY_ID, session(SESSION_ID, DAY_ID, WEDNESDAY, "2026-10-14T18:30:00Z"));

        LoggedSet saved = service(WEDNESDAY_NOON).saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 20, null, null, false));

        assertThat(saved).isEqualTo(new LoggedSet(20, null, null, false));
    }

    @Test
    void aCompletedSetNeedsReps() {
        assertApiError(() -> service(WEDNESDAY_NOON).saveSet(SESSION_ID,
                new SaveSetRequest(SQUAT, 1, new BigDecimal("60"), null, true)), HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.callsWith(UPSERT_SET)).isEmpty();
    }

    @Test
    void outOfRangeValuesAreRejected() {
        WorkoutSessionService service = service(WEDNESDAY_NOON);

        assertApiError(() -> service.saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 0, null, 10, false)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service.saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 21, null, 10, false)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service.saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 1, new BigDecimal("1000"), 1, true)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service.saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 1, new BigDecimal("-0.5"), 1, true)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service.saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 1, new BigDecimal("60.125"), 1, true)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service.saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 1, null, 1000, true)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service.saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 1, null, -1, true)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service.saveSet(SESSION_ID, new SaveSetRequest(null, 1, null, 1, true)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service.saveSet(SESSION_ID, null), HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void theLimitsThemselvesAreValid() {
        WorkoutSessionService service = service(WEDNESDAY_NOON);

        service.saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 1, new BigDecimal("999.99"), 999, true));
        service.saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 1, BigDecimal.ZERO, 0, true));

        assertThat(jdbc.callsWith(UPSERT_SET)).hasSize(2);
    }

    @Test
    void anExerciseOutsideTheSessionDayIsRejected() {
        jdbc.scalar(IS_IN_DAY, 0);

        assertApiError(() -> service(WEDNESDAY_NOON).saveSet(SESSION_ID, new SaveSetRequest(99, 1, null, 10, true)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.callsWith(UPSERT_SET)).isEmpty();
    }

    @Test
    void aSessionWhoseDayWasRemovedTakesNoNewSets() {
        jdbc.rows(SESSION_BY_ID, session(SESSION_ID, null, WEDNESDAY, null));

        assertApiError(() -> service(WEDNESDAY_NOON).saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 1, null, 10, true)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.callsWith(UPSERT_SET)).isEmpty();
    }

    @Test
    void anUnknownSessionIsNotFound() {
        jdbc.rows(SESSION_BY_ID);

        assertApiError(() -> service(WEDNESDAY_NOON).saveSet(SESSION_ID, new SaveSetRequest(SQUAT, 1, null, 10, true)),
                HttpStatus.NOT_FOUND, "not_found");
        assertApiError(() -> service(WEDNESDAY_NOON).deleteSet(SESSION_ID, SQUAT, 4), HttpStatus.NOT_FOUND, "not_found");
    }

    @Test
    void deletingASetBindsItsKey() {
        service(WEDNESDAY_NOON).deleteSet(SESSION_ID, SQUAT, 4);

        assertThat(jdbc.callsWith(DELETE_SET).get(0).args()).containsExactly(SESSION_ID, SQUAT, 4);
    }

    @Test
    void finishingKeepsTheFirstFinishTime() {
        jdbc.rows(FINISH, session(SESSION_ID, DAY_ID, WEDNESDAY, "2026-10-14T09:00:00Z"));

        SessionReply finished = service(WEDNESDAY_NOON).finish(SESSION_ID);

        assertThat(finished.finishedAt()).isEqualTo(Instant.parse("2026-10-14T09:00:00Z"));
        assertThat(jdbc.callsWith(FINISH).get(0).args()).containsExactly(NOON, SESSION_ID);
    }

    @Test
    void finishingAnUnknownSessionIsNotFound() {
        assertApiError(() -> service(WEDNESDAY_NOON).finish(SESSION_ID), HttpStatus.NOT_FOUND, "not_found");
    }

    private WorkoutSessionService service(Clock clock) {
        return new WorkoutSessionService(new TrainingProgramDao(jdbc), new WorkoutSessionDao(jdbc), new SetLogDao(jdbc),
                clock);
    }
}
