package com.magomez.androidapps.jcfit.admin;

import java.util.List;

/** The workout of one day of the week (1 = Monday ... 7 = Sunday), exercises by position. */
public record AdminProgramDay(int id, int dayOfWeek, String name, String muscles, List<AdminProgramExercise> exercises) {
}
