package com.magomez.androidapps.jcfit.training;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.magomez.androidapps.jcfit.training.DayAssemblerTest.set;
import static org.assertj.core.api.Assertions.assertThat;

/** The history of one exercise: best set, per-session totals and the limit. Pure: no database. */
class HistoryCalculatorTest {

    private static final int SQUAT = 4;
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_8 = LocalDate.of(2026, 10, 8);
    private static final LocalDate OCT_15 = LocalDate.of(2026, 10, 15);

    @Test
    void bestIsTheHeaviestThenTheMostRepsThenTheOldest() {
        List<TrainingRows.SetRow> sets = List.of(
                set(44, OCT_15, SQUAT, 1, "62.5", 10, true),
                set(37, OCT_8, SQUAT, 1, "62.5", 10, true),
                set(37, OCT_8, SQUAT, 2, "62.50", 8, true),
                set(30, OCT_1, SQUAT, 1, "60", 12, true));

        assertThat(HistoryCalculator.best(sets)).isEqualTo(new BestSet(new BigDecimal("62.5"), 10, OCT_8));
    }

    @Test
    void moreRepsWinAtTheSameWeight() {
        List<TrainingRows.SetRow> sets = List.of(
                set(30, OCT_1, SQUAT, 1, "60", 8, true),
                set(44, OCT_15, SQUAT, 1, "60", 9, true));

        assertThat(HistoryCalculator.best(sets)).isEqualTo(new BestSet(new BigDecimal("60"), 9, OCT_15));
    }

    @Test
    void aSetWithoutWeightOnlyWinsWhenNothingHasWeight() {
        assertThat(HistoryCalculator.best(List.of(set(30, OCT_1, SQUAT, 1, null, 20, true),
                set(44, OCT_15, SQUAT, 1, "2.5", 5, true))).weightKg()).isEqualByComparingTo("2.5");
        assertThat(HistoryCalculator.best(List.of(set(30, OCT_1, SQUAT, 1, null, 20, true))))
                .isEqualTo(new BestSet(null, 20, OCT_1));
    }

    @Test
    void bestIsNullWithoutSets() {
        assertThat(HistoryCalculator.best(List.of())).isNull();
    }

    @Test
    void sessionsGoFromNewestToOldestWithTheirTotals() {
        List<TrainingRows.SetRow> sets = List.of(
                set(30, OCT_1, SQUAT, 1, "60", 10, true),
                set(44, OCT_15, SQUAT, 2, "62.5", 8, true),
                set(44, OCT_15, SQUAT, 1, "62.5", 10, true),
                set(44, OCT_15, SQUAT, 3, null, 12, true));

        ExerciseHistory history = HistoryCalculator.history(SQUAT, "Sentadilla", sets, 20);

        assertThat(history.exerciseId()).isEqualTo(SQUAT);
        assertThat(history.name()).isEqualTo("Sentadilla");
        assertThat(history.sessionCount()).isEqualTo(2);
        assertThat(history.sessions()).extracting(HistorySession::sessionId).containsExactly(44, 30);

        HistorySession latest = history.sessions().get(0);
        assertThat(latest.date()).isEqualTo(OCT_15);
        assertThat(latest.dayName()).isEqualTo("Pierna");
        assertThat(latest.topWeightKg()).isEqualByComparingTo("62.5");
        assertThat(latest.volumeKg()).isEqualByComparingTo("1125");
        assertThat(latest.totalReps()).isEqualTo(30);
        assertThat(latest.sets()).extracting(SetEntry::setNumber).containsExactly(1, 2, 3);
    }

    @Test
    void aSessionWithoutWeightsHasNoTopWeightAndZeroVolume() {
        HistorySession session = HistoryCalculator.history(SQUAT, "Dominadas",
                List.of(set(30, OCT_1, SQUAT, 1, null, 10, true)), 20).sessions().get(0);

        assertThat(session.topWeightKg()).isNull();
        assertThat(session.volumeKg()).isEqualByComparingTo("0");
        assertThat(session.totalReps()).isEqualTo(10);
    }

    @Test
    void theLimitCutsTheSessionsButNotTheCountNorTheBest() {
        List<TrainingRows.SetRow> sets = List.of(
                set(30, OCT_1, SQUAT, 1, "80", 5, true),
                set(37, OCT_8, SQUAT, 1, "60", 10, true),
                set(44, OCT_15, SQUAT, 1, "62.5", 10, true));

        ExerciseHistory history = HistoryCalculator.history(SQUAT, "Sentadilla", sets, 2);

        assertThat(history.sessionCount()).isEqualTo(3);
        assertThat(history.sessions()).extracting(HistorySession::sessionId).containsExactly(44, 37);
        assertThat(history.best()).isEqualTo(new BestSet(new BigDecimal("80"), 5, OCT_1));
    }

    @Test
    void setsNotCompletedAreLeftOut() {
        ExerciseHistory history = HistoryCalculator.history(SQUAT, "Sentadilla",
                List.of(set(30, OCT_1, SQUAT, 1, "200", 1, false)), 20);

        assertThat(history.sessionCount()).isZero();
        assertThat(history.sessions()).isEmpty();
        assertThat(history.best()).isNull();
    }
}
