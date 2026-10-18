package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.YouTubeUrls;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Puts a training day's rows together into {@link WorkoutDayView}. Pure: no database. */
final class DayAssembler {

    /** Latest session first; on the same date, the one created last. */
    static final Comparator<TrainingRows.SetRow> NEWEST_SESSION_FIRST =
            Comparator.comparing(TrainingRows.SetRow::date).thenComparingInt(TrainingRows.SetRow::sessionId).reversed();

    private DayAssembler() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /**
     * session is null when the day is not started; logged are the sets of that session and
     * previous the earlier completed sets of the day's exercises.
     */
    static WorkoutDayView day(TrainingRows.ActiveDay activeDay, LocalDate date, TrainingRows.Session session,
                              LocalDate lastDate, List<TrainingRows.PlannedExercise> exercises,
                              List<TrainingRows.SetRow> logged, List<TrainingRows.SetRow> previous) {
        TrainingRows.Day day = activeDay.day();
        Map<Integer, List<TrainingRows.SetRow>> loggedByExercise = logged.stream()
                .collect(Collectors.groupingBy(TrainingRows.SetRow::exerciseId));
        List<DayExercise> dayExercises = exercises.stream()
                .sorted(Comparator.comparingInt(TrainingRows.PlannedExercise::position))
                .map(e -> exercise(e, previous(e.exerciseId(), date, previous),
                        loggedByExercise.getOrDefault(e.exerciseId(), List.of())))
                .toList();
        return new WorkoutDayView(day.id(), date, day.dayOfWeek(),
                TrainingDates.weekNumber(activeDay.programStart(), date), day.name(), day.muscles(),
                session == null ? null : session.id(),
                session == null ? null : session.startedAt(),
                session == null ? null : session.finishedAt(),
                lastDate, dayExercises);
    }

    /** The completed sets of the latest session before date that has any of the exercise; null if none. */
    static PreviousSets previous(int exerciseId, LocalDate date, List<TrainingRows.SetRow> candidates) {
        List<TrainingRows.SetRow> earlier = candidates.stream()
                .filter(s -> s.exerciseId() == exerciseId && s.completed() && s.date().isBefore(date))
                .toList();
        return earlier.stream().min(NEWEST_SESSION_FIRST)
                .map(latest -> new PreviousSets(latest.date(), earlier.stream()
                        .filter(s -> s.sessionId() == latest.sessionId())
                        .sorted(Comparator.comparingInt(TrainingRows.SetRow::setNumber))
                        .map(DayAssembler::entry)
                        .toList()))
                .orElse(null);
    }

    static SetEntry entry(TrainingRows.SetRow set) {
        return new SetEntry(set.setNumber(), set.weightKg(), set.reps());
    }

    private static DayExercise exercise(TrainingRows.PlannedExercise e, PreviousSets previous,
                                        List<TrainingRows.SetRow> logged) {
        return new DayExercise(e.id(), e.exerciseId(), e.position(), e.name(), e.youtubeUrl(),
                YouTubeUrls.videoId(e.youtubeUrl()).orElse(null), e.instructions(), e.sets(), e.repsMin(),
                e.repsMax(), e.restSeconds(), previous,
                logged.stream()
                        .sorted(Comparator.comparingInt(TrainingRows.SetRow::setNumber))
                        .map(s -> new LoggedSet(s.setNumber(), s.weightKg(), s.reps(), s.completed()))
                        .toList());
    }
}
