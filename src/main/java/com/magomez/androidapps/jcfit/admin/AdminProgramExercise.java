package com.magomez.androidapps.jcfit.admin;

/** One line of a day. archived tells the backoffice the exercise must be replaced on the next edit. */
public record AdminProgramExercise(int id, int exerciseId, String exerciseName, boolean archived, int position,
                                   int sets, int repsMin, int repsMax, Integer restSeconds) {
}
