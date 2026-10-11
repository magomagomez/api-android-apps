package com.magomez.androidapps.escapersthings.v2.rooms;

import com.magomez.androidapps.escapersthings.v2.config.ApiException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Scores written through v2: null, or 0 to 10 in quarter points (terror 0 means "no fear" and
 * is a real score). Only writes are checked; older scores off the step are still read.
 */
final class GradeValidator {

    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = BigDecimal.TEN;
    private static final BigDecimal STEPS_PER_POINT = BigDecimal.valueOf(4);
    private static final String INVALID_GRADE = "invalid_grade";

    private GradeValidator() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static Grade grade(Grade grade) {
        if (grade == null) {
            throw invalid("Faltan las notas");
        }
        check(grade.enigma(), "enigmas");
        check(grade.immersion(), "inmersión");
        check(grade.terror(), "terror");
        check(grade.gameMaster(), "game master");
        check(grade.global(), "global");
        return grade;
    }

    static boolean isValid(BigDecimal score) {
        return score == null
                || score.compareTo(MIN) >= 0 && score.compareTo(MAX) <= 0
                && score.multiply(STEPS_PER_POINT).stripTrailingZeros().scale() <= 0;
    }

    private static void check(BigDecimal score, String criterion) {
        if (!isValid(score)) {
            throw invalid("La nota de " + criterion + " debe ir de 0 a 10 en pasos de 0,25");
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, INVALID_GRADE, message);
    }
}
