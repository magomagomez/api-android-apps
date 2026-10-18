package com.magomez.androidapps.jcfit.training;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.IntStream;

import static com.magomez.androidapps.jcfit.training.TrainingFixtures.EXERCISE_NAME;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.HISTORY_SETS;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.assertApiError;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.row;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.set;
import static org.assertj.core.api.Assertions.assertThat;

/** GET /exercises/{id}/history?limit=. */
class ExerciseHistoryServiceTest {

    private static final int SQUAT = 4;
    private static final int SESSIONS = 25;

    private RecordingJdbcTemplate jdbc;
    private ExerciseHistoryService service;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        Map<String, Object>[] sets = IntStream.range(0, SESSIONS)
                .mapToObj(i -> set(100 - i, LocalDate.of(2026, 10, 1).minusDays(i), SQUAT, 1, "60", 10, true))
                .toArray(Map[]::new);
        jdbc = new RecordingJdbcTemplate()
                .rows(EXERCISE_NAME, row("name", "Sentadilla"))
                .rows(HISTORY_SETS, sets);
        service = new ExerciseHistoryService(new TrainingProgramDao(jdbc), new SetLogDao(jdbc));
    }

    @Test
    void theDefaultLimitIsTwenty() {
        ExerciseHistory history = service.history(SQUAT, null);

        assertThat(history.name()).isEqualTo("Sentadilla");
        assertThat(history.sessions()).hasSize(20);
        assertThat(history.sessionCount()).isEqualTo(SESSIONS);
        assertThat(jdbc.callsWith(HISTORY_SETS).get(0).args()).containsExactly(SQUAT);
    }

    @Test
    void theLimitIsApplied() {
        assertThat(service.history(SQUAT, 1).sessions()).hasSize(1);
        assertThat(service.history(SQUAT, 100).sessions()).hasSize(SESSIONS);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void aLimitOutOfRangeIsRejected(int limit) {
        assertApiError(() -> service.history(SQUAT, limit), HttpStatus.BAD_REQUEST, "bad_request");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void anUnknownExerciseIsNotFound() {
        jdbc.rows(EXERCISE_NAME);

        assertApiError(() -> service.history(SQUAT, null), HttpStatus.NOT_FOUND, "not_found");
    }
}
