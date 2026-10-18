package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.springframework.stereotype.Service;

/** "Historial": the completed sets of one exercise, session by session. */
@Service
public class ExerciseHistoryService {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;

    private final TrainingProgramDao programDao;
    private final SetLogDao setLogDao;

    public ExerciseHistoryService(TrainingProgramDao programDao, SetLogDao setLogDao) {
        this.programDao = programDao;
        this.setLogDao = setLogDao;
    }

    /** limit null means {@value #DEFAULT_LIMIT}; best and sessionCount look at every session. */
    public ExerciseHistory history(int exerciseId, Integer limit) {
        int sessions = limit == null ? DEFAULT_LIMIT : limit;
        if (sessions < 1 || sessions > MAX_LIMIT) {
            throw ApiException.badRequest("El límite tiene que ir de 1 a 100");
        }
        String name = programDao.findExerciseName(exerciseId).orElseThrow(TrainingErrors::exerciseNotFound);
        return HistoryCalculator.history(exerciseId, name, setLogDao.findCompleted(exerciseId), sessions);
    }
}
