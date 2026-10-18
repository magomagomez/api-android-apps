package com.magomez.androidapps.jcfit.admin;

/** Exercise of the catalogue as the backoffice shows it; youtubeVideoId is null without a valid link. */
public record AdminExercise(int id, String name, String youtubeUrl, String youtubeVideoId, String instructions,
                            String muscleGroup, boolean archived) {
}
