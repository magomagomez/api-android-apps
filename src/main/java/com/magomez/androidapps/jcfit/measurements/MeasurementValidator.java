package com.magomez.androidapps.jcfit.measurements;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

/**
 * Checks POST and PUT /measurements and returns the body with the notes trimmed (blank as null).
 * Values with more decimals than the column holds are rejected, never rounded.
 */
final class MeasurementValidator {

    static final int MAX_NOTES = 1000;
    private static final int KG_DECIMALS = 2;
    private static final int CM_DECIMALS = 1;
    private static final int PCT_DECIMALS = 1;
    private static final BigDecimal MIN_KG = BigDecimal.valueOf(20);
    private static final BigDecimal MAX_KG = BigDecimal.valueOf(400);
    private static final BigDecimal MIN_PCT = BigDecimal.valueOf(2);
    private static final BigDecimal MAX_PCT = BigDecimal.valueOf(70);
    private static final BigDecimal MIN_CM = BigDecimal.valueOf(10);
    private static final BigDecimal MAX_CM = BigDecimal.valueOf(300);
    private static final String FUTURE_DATE = "future_date";

    private MeasurementValidator() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static MeasurementRequest measurement(MeasurementRequest request, LocalDate today) {
        if (request == null) {
            throw ApiException.badRequest("Faltan los datos de la medida");
        }
        if (request.measuredOn() == null) {
            throw ApiException.badRequest("La fecha es obligatoria");
        }
        if (request.measuredOn().isAfter(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, FUTURE_DATE, "No puedes registrar medidas de un día futuro");
        }
        boolean anyValue = Stream.of(request.weightKg(), request.bodyFatPct(), request.chestCm(), request.waistCm(),
                request.hipCm(), request.legCm()).anyMatch(value -> value != null);
        if (!anyValue) {
            throw ApiException.badRequest("Rellena al menos una medida");
        }
        check(request.weightKg(), "El peso", MIN_KG, MAX_KG, KG_DECIMALS, "kg");
        check(request.bodyFatPct(), "La grasa corporal", MIN_PCT, MAX_PCT, PCT_DECIMALS, "%");
        check(request.chestCm(), "El pecho", MIN_CM, MAX_CM, CM_DECIMALS, "cm");
        check(request.waistCm(), "La cintura", MIN_CM, MAX_CM, CM_DECIMALS, "cm");
        check(request.hipCm(), "La cadera", MIN_CM, MAX_CM, CM_DECIMALS, "cm");
        check(request.legCm(), "La pierna", MIN_CM, MAX_CM, CM_DECIMALS, "cm");
        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().trim();
        if (notes != null && notes.codePointCount(0, notes.length()) > MAX_NOTES) {
            throw ApiException.badRequest("Las notas no pueden pasar de " + MAX_NOTES + " caracteres");
        }
        return new MeasurementRequest(request.measuredOn(), request.weightKg(), request.bodyFatPct(), request.chestCm(),
                request.waistCm(), request.hipCm(), request.legCm(), notes);
    }

    /** Null is fine: every value is optional on its own. */
    private static void check(BigDecimal value, String field, BigDecimal min, BigDecimal max, int decimals,
                              String unit) {
        if (value == null) {
            return;
        }
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw ApiException.badRequest(field + " tiene que estar entre " + min + " y " + max + " " + unit);
        }
        if (value.stripTrailingZeros().scale() > decimals) {
            throw ApiException.badRequest(field + " admite como mucho " + decimals
                    + (decimals == 1 ? " decimal" : " decimales"));
        }
    }
}
