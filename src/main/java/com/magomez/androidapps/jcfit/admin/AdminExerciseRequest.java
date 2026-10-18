package com.magomez.androidapps.jcfit.admin;

/** Body of POST and PUT /admin/exercises. archived is only read by PUT; null keeps the current value. */
public record AdminExerciseRequest(String name, String youtubeUrl, String instructions, String muscleGroup,
                                   Boolean archived) {
}
