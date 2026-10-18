package com.magomez.androidapps.jcfit.training;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.magomez.androidapps.jcfit.training.TrainingFixtures.PROGRAM_START;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

/** The seven days of a week with their status and completed exercises. Pure: no database. */
class WeekCalculatorTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);
    private static final LocalDate THURSDAY = MONDAY.plusDays(3);
    private static final Instant STARTED = Instant.parse("2026-10-12T17:00:00Z");
    private static final Instant FINISHED = Instant.parse("2026-10-12T18:00:00Z");

    // Training on Monday (1), Tuesday (2), Wednesday (3) and Thursday (4); the rest is rest.
    private static final List<TrainingRows.Day> DAYS = List.of(
            new TrainingRows.Day(3, 1, "Empuje", "Pecho"),
            new TrainingRows.Day(4, 2, "Tirón", "Espalda"),
            new TrainingRows.Day(5, 3, "Pierna", "Cuádriceps"),
            new TrainingRows.Day(6, 4, "Torso", null));
    private static final List<TrainingRows.PlannedExercise> EXERCISES = List.of(
            planned(11, 3, 40, 3), planned(12, 3, 41, 4),
            planned(13, 4, 42, 3),
            planned(14, 5, 43, 3), planned(15, 5, 44, 2),
            planned(16, 6, 45, 3));

    @Test
    void everyStatusOfTheWeek() {
        List<TrainingRows.Session> sessions = List.of(
                new TrainingRows.Session(41, 3, "Empuje", MONDAY, STARTED, FINISHED),
                new TrainingRows.Session(42, 4, "Tirón", TUESDAY, STARTED, null),
                new TrainingRows.Session(43, 5, "Pierna", WEDNESDAY, STARTED, null));

        WeekView week = WeekCalculator.week(PROGRAM_START, THURSDAY, WEDNESDAY, DAYS, EXERCISES, sessions, List.of());

        assertThat(week.days()).extracting(WeekDay::status).containsExactly(DayStatus.DONE, DayStatus.MISSED,
                DayStatus.TODAY, DayStatus.PENDING, DayStatus.REST, DayStatus.REST, DayStatus.REST);
        assertThat(week.days()).extracting(WeekDay::sessionId).containsExactly(41, 42, 43, null, null, null, null);
        assertThat(week.planned()).isEqualTo(4);
        assertThat(week.completed()).isEqualTo(1);
    }

    @Test
    void theWeekGoesFromMondayToSundayWithItsNumber() {
        WeekView week = WeekCalculator.week(PROGRAM_START, THURSDAY, WEDNESDAY, DAYS, EXERCISES, List.of(), List.of());

        assertThat(week.weekNumber()).isEqualTo(7);
        assertThat(week.startDate()).isEqualTo(MONDAY);
        assertThat(week.endDate()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(week.today()).isEqualTo(WEDNESDAY);
        assertThat(week.days()).extracting(WeekDay::dayOfWeek).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(week.days()).extracting(WeekDay::date).containsExactly(MONDAY, TUESDAY, WEDNESDAY, THURSDAY,
                MONDAY.plusDays(4), MONDAY.plusDays(5), MONDAY.plusDays(6));
        assertThat(week.days().get(2).status()).isEqualTo(DayStatus.TODAY);
        assertThat(week.days().get(2).sessionId()).isNull();
    }

    @Test
    void daysBeforeTheProgramStartsAreRestInItsFirstWeek() {
        WeekView week = WeekCalculator.week(WEDNESDAY, MONDAY, THURSDAY, DAYS, EXERCISES, List.of(), List.of());

        assertThat(week.weekNumber()).isEqualTo(1);
        assertThat(week.days()).extracting(WeekDay::status).containsExactly(DayStatus.REST, DayStatus.REST,
                DayStatus.MISSED, DayStatus.TODAY, DayStatus.REST, DayStatus.REST, DayStatus.REST);
        assertThat(week.days().get(0).programDayId()).isNull();
        assertThat(week.planned()).isEqualTo(2);
    }

    @Test
    void aRestDayHasNoTraining() {
        WeekDay saturday = WeekCalculator.week(PROGRAM_START, THURSDAY, WEDNESDAY, DAYS, EXERCISES, List.of(),
                List.of()).days().get(5);

        assertThat(saturday).isEqualTo(new WeekDay(MONDAY.plusDays(5), 6, DayStatus.REST, null, null, null, 0, 0,
                null));
    }

    @Test
    void aTrainingDayCarriesItsProgramData() {
        WeekDay monday = WeekCalculator.week(PROGRAM_START, THURSDAY, WEDNESDAY, DAYS, EXERCISES, List.of(),
                List.of()).days().get(0);

        assertThat(monday.programDayId()).isEqualTo(3);
        assertThat(monday.name()).isEqualTo("Empuje");
        assertThat(monday.muscles()).isEqualTo("Pecho");
        assertThat(monday.exerciseCount()).isEqualTo(2);
        assertThat(monday.status()).isEqualTo(DayStatus.MISSED);
    }

    @Test
    void anExerciseIsCompletedWithAtLeastTheSetsTheProgramAsks() {
        List<TrainingRows.Session> sessions = List.of(new TrainingRows.Session(41, 3, "Empuje", MONDAY, STARTED, null));
        List<TrainingRows.CompletedCount> counts = List.of(
                new TrainingRows.CompletedCount(41, 40, 5),   // 3 asked, 5 done: completed
                new TrainingRows.CompletedCount(41, 41, 3),   // 4 asked, 3 done: not yet
                new TrainingRows.CompletedCount(99, 41, 4));  // another session's sets do not count

        WeekDay monday = WeekCalculator.week(PROGRAM_START, THURSDAY, WEDNESDAY, DAYS, EXERCISES, sessions, counts)
                .days().get(0);

        assertThat(monday.completedExercises()).isEqualTo(1);
        assertThat(monday.exerciseCount()).isEqualTo(2);
    }

    @Test
    void exactlyTheAskedSetsCompletesTheExercise() {
        List<TrainingRows.Session> sessions = List.of(new TrainingRows.Session(41, 3, "Empuje", MONDAY, STARTED, null));
        List<TrainingRows.CompletedCount> counts = List.of(new TrainingRows.CompletedCount(41, 40, 3),
                new TrainingRows.CompletedCount(41, 41, 4));

        WeekDay monday = WeekCalculator.week(PROGRAM_START, THURSDAY, WEDNESDAY, DAYS, EXERCISES, sessions, counts)
                .days().get(0);

        assertThat(monday.completedExercises()).isEqualTo(2);
    }

    @Test
    void aSessionOfAnotherDayOrDateIsIgnored() {
        List<TrainingRows.Session> sessions = List.of(
                new TrainingRows.Session(50, 4, "Tirón", MONDAY, STARTED, FINISHED),
                new TrainingRows.Session(51, null, "Antiguo", MONDAY, STARTED, FINISHED));

        WeekDay monday = WeekCalculator.week(PROGRAM_START, THURSDAY, WEDNESDAY, DAYS, EXERCISES, sessions, List.of())
                .days().get(0);

        assertThat(monday.status()).isEqualTo(DayStatus.MISSED);
        assertThat(monday.sessionId()).isNull();
    }

    @Test
    void aFinishedSessionTodayIsDone() {
        List<TrainingRows.Session> sessions = List.of(new TrainingRows.Session(43, 5, "Pierna", WEDNESDAY, STARTED,
                FINISHED));

        WeekView week = WeekCalculator.week(PROGRAM_START, WEDNESDAY, WEDNESDAY, DAYS, EXERCISES, sessions, List.of());

        assertThat(week.days().get(2).status()).isEqualTo(DayStatus.DONE);
        assertThat(week.completed()).isEqualTo(1);
    }

    private static TrainingRows.PlannedExercise planned(int id, int dayId, int exerciseId, int sets) {
        return new TrainingRows.PlannedExercise(id, dayId, exerciseId, id, "Ejercicio " + exerciseId, null, null,
                sets, 8, 10, 90);
    }
}
