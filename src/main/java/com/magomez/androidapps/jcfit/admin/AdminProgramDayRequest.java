package com.magomez.androidapps.jcfit.admin;

import java.util.List;

/** Body of PUT /admin/program/days/{dayOfWeek}: the whole day, exercises in the order to keep. */
public record AdminProgramDayRequest(String name, String muscles, List<AdminProgramExerciseRequest> exercises) {
}
