package com.magomez.androidapps.jcfit.training;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** "Entreno del día": one program day on one date, with what was done before and what is logged. */
@Service
public class WorkoutDayService {

    private final TrainingProgramDao programDao;
    private final WorkoutSessionDao sessionDao;
    private final SetLogDao setLogDao;

    public WorkoutDayService(TrainingProgramDao programDao, WorkoutSessionDao sessionDao, SetLogDao setLogDao) {
        this.programDao = programDao;
        this.sessionDao = sessionDao;
        this.setLogDao = setLogDao;
    }

    /** A fixed number of queries whatever the exercises of the day. */
    public WorkoutDayView day(int programDayId, LocalDate date) {
        TrainingRows.ActiveDay activeDay = programDao.findActiveDay(programDayId)
                .orElseThrow(TrainingErrors::dayNotFound);
        TrainingDates.requireDayOfWeek(activeDay.day().dayOfWeek(), date);
        TrainingDates.weekNumber(activeDay.programStart(), date);
        List<TrainingRows.PlannedExercise> exercises = programDao.findDayExercises(programDayId);
        TrainingRows.Session session = sessionDao.find(programDayId, date).orElse(null);
        List<TrainingRows.SetRow> logged = session == null ? List.of() : setLogDao.findBySession(session.id());
        List<TrainingRows.SetRow> previous = setLogDao.findPrevious(programDayId, date);
        LocalDate lastDate = sessionDao.findLastDate(programDayId, date).orElse(null);
        return DayAssembler.day(activeDay, date, session, lastDate, exercises, logged, previous);
    }
}
