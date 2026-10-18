package com.magomez.androidapps.jcfit.measurements;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One day's weight, body fat and perimeters; every value is optional, at least one is present. */
public record Measurement(int id,
                          @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                          LocalDate measuredOn,
                          BigDecimal weightKg, BigDecimal bodyFatPct, BigDecimal chestCm, BigDecimal waistCm,
                          BigDecimal hipCm, BigDecimal legCm, String notes, List<MeasurementPhoto> photos) {
}
