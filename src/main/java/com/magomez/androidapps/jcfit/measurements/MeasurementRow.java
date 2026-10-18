package com.magomez.androidapps.jcfit.measurements;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A jcfit_measurements row; {@link #withPhotos} turns it into what the app gets. */
record MeasurementRow(int id, LocalDate measuredOn, BigDecimal weightKg, BigDecimal bodyFatPct, BigDecimal chestCm,
                      BigDecimal waistCm, BigDecimal hipCm, BigDecimal legCm, String notes) {

    Measurement withPhotos(List<MeasurementPhoto> photos) {
        return new Measurement(id, measuredOn, weightKg, bodyFatPct, chestCm, waistCm, hipCm, legCm, notes, photos);
    }
}
