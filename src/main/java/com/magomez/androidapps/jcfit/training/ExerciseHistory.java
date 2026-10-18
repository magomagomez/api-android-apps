package com.magomez.androidapps.jcfit.training;

import java.util.List;

/** GET /exercises/{id}/history: sessionCount counts every session, not only the ones returned. */
public record ExerciseHistory(int exerciseId, String name, BestSet best, int sessionCount,
                              List<HistorySession> sessions) {
}
