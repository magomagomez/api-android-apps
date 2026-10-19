package com.magomez.androidapps.jcfit.progress;

import com.magomez.androidapps.jcfit.training.SetEntry;

import java.util.List;

/** An exercise of {@link AdminSessionSummary} with its completed sets by set number. */
public record AdminSessionExercise(int exerciseId, String name, List<SetEntry> sets) {
}
