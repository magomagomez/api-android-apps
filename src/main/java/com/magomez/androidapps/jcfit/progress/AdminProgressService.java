package com.magomez.androidapps.jcfit.progress;

import com.magomez.androidapps.jcfit.config.ApiException;
import com.magomez.androidapps.jcfit.training.SetEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cris's latest sessions for the backoffice, read-only. */
@Service
public class AdminProgressService {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;
    private static final String LIMIT_MESSAGE = "El límite tiene que ir de 1 a " + MAX_LIMIT;

    private final AdminProgressDao progressDao;

    public AdminProgressService(AdminProgressDao progressDao) {
        this.progressDao = progressDao;
    }

    /** limit null means {@value #DEFAULT_LIMIT}; newest session first, sessions without sets included. */
    public List<AdminSessionSummary> sessions(Integer limit) {
        int rows = limit == null ? DEFAULT_LIMIT : limit;
        if (rows < 1 || rows > MAX_LIMIT) {
            throw ApiException.badRequest(LIMIT_MESSAGE);
        }
        Map<Integer, SessionBuilder> sessions = new LinkedHashMap<>();
        for (AdminProgressRow row : progressDao.findRecentSessions(rows)) {
            SessionBuilder session = sessions.computeIfAbsent(row.sessionId(), id -> new SessionBuilder(row));
            if (row.exerciseId() != null) {
                session.exercises.computeIfAbsent(row.exerciseId(), id -> new ExerciseBuilder(row.exerciseName()))
                        .sets.add(new SetEntry(row.setNumber(), row.weightKg(), row.reps()));
            }
        }
        return sessions.values().stream().map(SessionBuilder::build).toList();
    }

    private static final class SessionBuilder {

        private final AdminProgressRow first;
        private final Map<Integer, ExerciseBuilder> exercises = new LinkedHashMap<>();

        private SessionBuilder(AdminProgressRow first) {
            this.first = first;
        }

        private AdminSessionSummary build() {
            List<AdminSessionExercise> built = exercises.entrySet().stream()
                    .map(entry -> new AdminSessionExercise(entry.getKey(), entry.getValue().name,
                            entry.getValue().sets.stream().sorted(Comparator.comparingInt(SetEntry::setNumber)).toList()))
                    .toList();
            return new AdminSessionSummary(first.sessionId(), first.date(), first.dayName(), first.finished(), built);
        }
    }

    private static final class ExerciseBuilder {

        private final String name;
        private final List<SetEntry> sets = new ArrayList<>();

        private ExerciseBuilder(String name) {
            this.name = name;
        }
    }
}
