package com.magomez.androidapps.jcfit.admin;

/** One line of {@link AdminProgramDayRequest}; boxed so a missing field is a 400, not a 0. */
public record AdminProgramExerciseRequest(Integer exerciseId, Integer sets, Integer repsMin, Integer repsMax,
                                          Integer restSeconds) {
}
