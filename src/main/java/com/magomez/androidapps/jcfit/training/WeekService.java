package com.magomez.androidapps.jcfit.training;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/** "Semana": the seven days of a week of the active program with their status. */
@Service
public class WeekService {

    private final TrainingProgramDao programDao;
    private final WorkoutSessionDao sessionDao;
    private final SetLogDao setLogDao;
    private final Clock clock;

    public WeekService(TrainingProgramDao programDao, WorkoutSessionDao sessionDao, SetLogDao setLogDao,
                       @Qualifier("jcfitClock") Clock clock) {
        this.programDao = programDao;
        this.sessionDao = sessionDao;
        this.setLogDao = setLogDao;
        this.clock = clock;
    }

    /** date is any day of the week asked for. */
    public WeekView week(LocalDate date) {
        TrainingRows.Program program = programDao.findActive().orElseThrow(TrainingErrors::noActiveProgram);
        TrainingDates.weekNumber(program.startDate(), date);
        LocalDate monday = TrainingDates.monday(date);
        LocalDate sunday = monday.plusDays(TrainingDates.DAYS_PER_WEEK - 1L);
        return WeekCalculator.week(program.startDate(), date, TrainingDates.today(clock),
                programDao.findDays(program.id()), programDao.findExercises(program.id()),
                sessionDao.findBetween(monday, sunday), setLogDao.countCompleted(monday, sunday));
    }
}
