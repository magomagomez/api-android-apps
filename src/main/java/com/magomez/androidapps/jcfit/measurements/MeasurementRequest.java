package com.magomez.androidapps.jcfit.measurements;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body of POST and PUT /measurements: a {@link Measurement} without id nor photos. */
public record MeasurementRequest(@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                                 LocalDate measuredOn,
                                 BigDecimal weightKg, BigDecimal bodyFatPct, BigDecimal chestCm, BigDecimal waistCm,
                                 BigDecimal hipCm, BigDecimal legCm, String notes) {
}
