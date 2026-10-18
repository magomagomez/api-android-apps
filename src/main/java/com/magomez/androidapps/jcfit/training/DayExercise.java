package com.magomez.androidapps.jcfit.training;

import java.util.List;

/** One exercise of {@link WorkoutDayView}; previous is null when it was never done before. */
public record DayExercise(int programExerciseId, int exerciseId, int position, String name, String youtubeUrl,
                          String youtubeVideoId, String instructions, int sets, int repsMin, int repsMax,
                          Integer restSeconds, PreviousSets previous, List<LoggedSet> logged) {
}
