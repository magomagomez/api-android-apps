package com.magomez.androidapps.jcfit.training;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** The history of one exercise from its set rows: best set and per-session totals. Pure: no database. */
final class HistoryCalculator {

    /** Heaviest first (no weight counts as 0), then most reps, then the oldest. */
    private static final Comparator<TrainingRows.SetRow> BEST_FIRST =
            Comparator.comparing(HistoryCalculator::weightOrZero).reversed()
                    .thenComparing(Comparator.comparing(HistoryCalculator::repsOrZero).reversed())
                    .thenComparing(TrainingRows.SetRow::date)
                    .thenComparingInt(TrainingRows.SetRow::sessionId)
                    .thenComparingInt(TrainingRows.SetRow::setNumber);

    private HistoryCalculator() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /** Only completed sets count; sessions go from the newest, cut to limit. */
    static ExerciseHistory history(int exerciseId, String name, List<TrainingRows.SetRow> sets, int limit) {
        List<TrainingRows.SetRow> completed = sets.stream().filter(TrainingRows.SetRow::completed).toList();
        Map<Integer, List<TrainingRows.SetRow>> bySession = completed.stream()
                .sorted(DayAssembler.NEWEST_SESSION_FIRST)
                .collect(Collectors.groupingBy(TrainingRows.SetRow::sessionId, LinkedHashMap::new,
                        Collectors.toList()));
        List<HistorySession> sessions = bySession.values().stream()
                .limit(limit)
                .map(HistoryCalculator::session)
                .toList();
        return new ExerciseHistory(exerciseId, name, best(completed), bySession.size(), sessions);
    }

    /** Null when there are no sets. */
    static BestSet best(List<TrainingRows.SetRow> sets) {
        return sets.stream().min(BEST_FIRST)
                .map(s -> new BestSet(s.weightKg(), s.reps(), s.date()))
                .orElse(null);
    }

    private static HistorySession session(List<TrainingRows.SetRow> sets) {
        TrainingRows.SetRow first = sets.get(0);
        BigDecimal topWeight = sets.stream().map(TrainingRows.SetRow::weightKg).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        BigDecimal volume = sets.stream()
                .map(s -> weightOrZero(s).multiply(BigDecimal.valueOf(repsOrZero(s))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalReps = sets.stream().mapToInt(HistoryCalculator::repsOrZero).sum();
        return new HistorySession(first.sessionId(), first.date(), first.dayName(), topWeight, volume, totalReps,
                sets.stream()
                        .sorted(Comparator.comparingInt(TrainingRows.SetRow::setNumber))
                        .map(DayAssembler::entry)
                        .toList());
    }

    private static BigDecimal weightOrZero(TrainingRows.SetRow set) {
        return set.weightKg() == null ? BigDecimal.ZERO : set.weightKg();
    }

    private static int repsOrZero(TrainingRows.SetRow set) {
        return set.reps() == null ? 0 : set.reps();
    }
}
