package com.magomez.androidapps.jcfit.training;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.magomez.androidapps.jcfit.training.TrainingFixtures.PROGRAM_START;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

/** One training day with its exercises, the previous sets and what is logged. Pure: no database. */
class DayAssemblerTest {

    private static final int DAY_ID = 5;
    private static final int SQUAT = 4;
    private static final int LUNGE = 7;
    private static final LocalDate LAST_WEEK = WEDNESDAY.minusDays(7);
    private static final LocalDate TWO_WEEKS_AGO = WEDNESDAY.minusDays(14);
    private static final TrainingRows.ActiveDay DAY = new TrainingRows.ActiveDay(
            new TrainingRows.Day(DAY_ID, 3, "Pierna", "Cuádriceps · Glúteo"), PROGRAM_START);
    private static final List<TrainingRows.PlannedExercise> EXERCISES = List.of(
            new TrainingRows.PlannedExercise(11, DAY_ID, SQUAT, 1, "Sentadilla", "https://youtu.be/dQw4w9WgXcQ",
                    "Espalda neutra", 3, 8, 10, 90),
            new TrainingRows.PlannedExercise(12, DAY_ID, LUNGE, 2, "Zancada", "https://vimeo.com/1", null, 2, 10, 12,
                    null));

    @Test
    void previousIsTheLatestEarlierSessionWithItsCompletedSetsOnly() {
        List<TrainingRows.SetRow> candidates = List.of(
                set(30, TWO_WEEKS_AGO, SQUAT, 1, "55", 10, true),
                set(37, LAST_WEEK, SQUAT, 2, "60", 8, true),
                set(37, LAST_WEEK, SQUAT, 1, "60", 10, true),
                set(37, LAST_WEEK, SQUAT, 3, "62.5", null, false));

        PreviousSets previous = DayAssembler.previous(SQUAT, WEDNESDAY, candidates);

        assertThat(previous.date()).isEqualTo(LAST_WEEK);
        assertThat(previous.sets()).containsExactly(new SetEntry(1, new BigDecimal("60"), 10),
                new SetEntry(2, new BigDecimal("60"), 8));
    }

    @Test
    void previousIgnoresTheSessionOfTheSameDayAndLaterOnes() {
        List<TrainingRows.SetRow> candidates = List.of(
                set(44, WEDNESDAY, SQUAT, 1, "65", 10, true),
                set(50, WEDNESDAY.plusDays(7), SQUAT, 1, "70", 10, true),
                set(37, LAST_WEEK, SQUAT, 1, "60", 10, true));

        assertThat(DayAssembler.previous(SQUAT, WEDNESDAY, candidates).date()).isEqualTo(LAST_WEEK);
    }

    @Test
    void previousSkipsASessionWithoutCompletedSetsOfTheExercise() {
        List<TrainingRows.SetRow> candidates = List.of(
                set(37, LAST_WEEK, SQUAT, 1, "60", 10, false),
                set(37, LAST_WEEK, LUNGE, 1, "20", 10, true),
                set(30, TWO_WEEKS_AGO, SQUAT, 1, "55", 10, true));

        assertThat(DayAssembler.previous(SQUAT, WEDNESDAY, candidates).date()).isEqualTo(TWO_WEEKS_AGO);
    }

    @Test
    void previousIsNullWhenTheExerciseWasNeverDone() {
        assertThat(DayAssembler.previous(SQUAT, WEDNESDAY, List.of())).isNull();
        assertThat(DayAssembler.previous(SQUAT, WEDNESDAY,
                List.of(set(37, LAST_WEEK, SQUAT, 1, "60", 10, false)))).isNull();
    }

    @Test
    void theDayListsItsExercisesInOrderWithWhatIsLogged() {
        TrainingRows.Session session = new TrainingRows.Session(44, DAY_ID, "Pierna", WEDNESDAY,
                Instant.parse("2026-10-14T17:40:00Z"), null);
        List<TrainingRows.SetRow> logged = List.of(
                set(44, WEDNESDAY, SQUAT, 2, null, null, false),
                set(44, WEDNESDAY, SQUAT, 1, "62.5", 10, true));
        List<TrainingRows.SetRow> previous = List.of(set(37, LAST_WEEK, SQUAT, 1, "60", 10, true));

        WorkoutDayView view = DayAssembler.day(DAY, WEDNESDAY, session, LAST_WEEK,
                List.of(EXERCISES.get(1), EXERCISES.get(0)), logged, previous);

        assertThat(view.programDayId()).isEqualTo(DAY_ID);
        assertThat(view.dayOfWeek()).isEqualTo(3);
        assertThat(view.weekNumber()).isEqualTo(7);
        assertThat(view.sessionId()).isEqualTo(44);
        assertThat(view.startedAt()).isEqualTo(Instant.parse("2026-10-14T17:40:00Z"));
        assertThat(view.finishedAt()).isNull();
        assertThat(view.lastDate()).isEqualTo(LAST_WEEK);
        assertThat(view.exercises()).extracting(DayExercise::position).containsExactly(1, 2);

        DayExercise squat = view.exercises().get(0);
        assertThat(squat.youtubeVideoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(squat.logged()).containsExactly(new LoggedSet(1, new BigDecimal("62.5"), 10, true),
                new LoggedSet(2, null, null, false));
        assertThat(squat.previous().date()).isEqualTo(LAST_WEEK);

        DayExercise lunge = view.exercises().get(1);
        assertThat(lunge.youtubeVideoId()).isNull();
        assertThat(lunge.restSeconds()).isNull();
        assertThat(lunge.previous()).isNull();
        assertThat(lunge.logged()).isEmpty();
    }

    @Test
    void aDayNotStartedHasNoSession() {
        WorkoutDayView view = DayAssembler.day(DAY, WEDNESDAY, null, null, EXERCISES, List.of(), List.of());

        assertThat(view.sessionId()).isNull();
        assertThat(view.startedAt()).isNull();
        assertThat(view.finishedAt()).isNull();
        assertThat(view.lastDate()).isNull();
        assertThat(view.exercises()).allSatisfy(exercise -> assertThat(exercise.logged()).isEmpty());
    }

    static TrainingRows.SetRow set(int sessionId, LocalDate date, int exerciseId, int setNumber, String weightKg,
                                   Integer reps, boolean completed) {
        return new TrainingRows.SetRow(sessionId, date, "Pierna", exerciseId, setNumber,
                weightKg == null ? null : new BigDecimal(weightKg), reps, completed);
    }
}
