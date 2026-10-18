package com.magomez.androidapps.jcfit.training;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Puts a week's rows together into {@link WeekView}. Pure: no database. */
final class WeekCalculator {

    private WeekCalculator() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /**
     * days and exercises are those of the active program; sessions and counts those of the
     * week of date.
     */
    static WeekView week(LocalDate programStart, LocalDate date, LocalDate today, List<TrainingRows.Day> days,
                         List<TrainingRows.PlannedExercise> exercises, List<TrainingRows.Session> sessions,
                         List<TrainingRows.CompletedCount> counts) {
        int weekNumber = TrainingDates.weekNumber(programStart, date);
        LocalDate monday = TrainingDates.monday(date);
        Map<Integer, TrainingRows.Day> byDayOfWeek = days.stream()
                .collect(Collectors.toMap(TrainingRows.Day::dayOfWeek, Function.identity()));
        Map<Integer, List<TrainingRows.PlannedExercise>> exercisesByDay = exercises.stream()
                .collect(Collectors.groupingBy(TrainingRows.PlannedExercise::programDayId));
        List<WeekDay> weekDays = new ArrayList<>();
        for (int offset = 0; offset < TrainingDates.DAYS_PER_WEEK; offset++) {
            LocalDate day = monday.plusDays(offset);
            int dayOfWeek = day.getDayOfWeek().getValue();
            // In the first week, the days before the program starts were never planned.
            TrainingRows.Day planned = day.isBefore(programStart) ? null : byDayOfWeek.get(dayOfWeek);
            weekDays.add(Optional.ofNullable(planned)
                    .map(programDay -> trainingDay(day, today, programDay,
                            exercisesByDay.getOrDefault(programDay.id(), List.of()), sessions, counts))
                    .orElseGet(() -> restDay(day)));
        }
        int planned = (int) weekDays.stream().filter(d -> d.status() != DayStatus.REST).count();
        int completed = (int) weekDays.stream().filter(d -> d.status() == DayStatus.DONE).count();
        return new WeekView(weekNumber, monday, monday.plusDays(TrainingDates.DAYS_PER_WEEK - 1L), today, planned,
                completed, weekDays);
    }

    static DayStatus status(TrainingRows.Session session, LocalDate date, LocalDate today) {
        if (session != null && session.finishedAt() != null) {
            return DayStatus.DONE;
        }
        if (date.equals(today)) {
            return DayStatus.TODAY;
        }
        return date.isBefore(today) ? DayStatus.MISSED : DayStatus.PENDING;
    }

    /** Exercises of the day with at least as many completed sets as the program asks. */
    static int completedExercises(List<TrainingRows.PlannedExercise> dayExercises, TrainingRows.Session session,
                                  List<TrainingRows.CompletedCount> counts) {
        if (session == null) {
            return 0;
        }
        Map<Integer, Integer> completedSets = counts.stream()
                .filter(c -> c.sessionId() == session.id())
                .collect(Collectors.toMap(TrainingRows.CompletedCount::exerciseId,
                        TrainingRows.CompletedCount::completedSets, Integer::sum));
        return (int) dayExercises.stream()
                .filter(e -> completedSets.getOrDefault(e.exerciseId(), 0) >= e.sets())
                .count();
    }

    private static WeekDay trainingDay(LocalDate date, LocalDate today, TrainingRows.Day programDay,
                                       List<TrainingRows.PlannedExercise> dayExercises,
                                       List<TrainingRows.Session> sessions, List<TrainingRows.CompletedCount> counts) {
        TrainingRows.Session session = sessions.stream()
                .filter(s -> Objects.equals(s.programDayId(), programDay.id()) && s.date().equals(date))
                .findFirst()
                .orElse(null);
        return new WeekDay(date, programDay.dayOfWeek(), status(session, date, today), programDay.id(),
                programDay.name(), programDay.muscles(), dayExercises.size(),
                completedExercises(dayExercises, session, counts), session == null ? null : session.id());
    }

    private static WeekDay restDay(LocalDate date) {
        return new WeekDay(date, date.getDayOfWeek().getValue(), DayStatus.REST, null, null, null, 0, 0, null);
    }
}
