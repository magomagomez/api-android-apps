package com.magomez.androidapps.jcfit.measurements;

import java.util.List;

/** GET /measurements: items newest first; first and latest are null when there are none. */
public record MeasurementHistory(Measurement first, Measurement latest, List<Measurement> items) {
}
