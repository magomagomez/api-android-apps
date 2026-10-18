package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.ApiException;

import java.math.BigDecimal;

/** Checks a set written through PUT /sessions/{id}/sets against the contract ranges. */
final class SetValidator {

    static final int MIN_SET_NUMBER = 1;
    static final int MAX_SET_NUMBER = 20;
    static final int MAX_REPS = 999;
    static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("999.99");
    static final int WEIGHT_DECIMALS = 2;

    private SetValidator() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static void validate(SaveSetRequest request) {
        if (request == null || request.exerciseId() == null || request.setNumber() == null) {
            throw ApiException.badRequest("Faltan el ejercicio o el número de serie");
        }
        if (request.setNumber() < MIN_SET_NUMBER || request.setNumber() > MAX_SET_NUMBER) {
            throw ApiException.badRequest("La serie tiene que ir de 1 a 20");
        }
        BigDecimal weight = request.weightKg();
        if (weight != null && (weight.signum() < 0 || weight.compareTo(MAX_WEIGHT_KG) > 0)) {
            throw ApiException.badRequest("El peso tiene que ir de 0 a 999,99 kg");
        }
        if (weight != null && weight.stripTrailingZeros().scale() > WEIGHT_DECIMALS) {
            throw ApiException.badRequest("El peso admite como mucho 2 decimales");
        }
        if (request.reps() != null && (request.reps() < 0 || request.reps() > MAX_REPS)) {
            throw ApiException.badRequest("Las repeticiones tienen que ir de 0 a 999");
        }
        if (completed(request) && request.reps() == null) {
            throw ApiException.badRequest("Una serie completada necesita las repeticiones");
        }
    }

    static boolean completed(SaveSetRequest request) {
        return Boolean.TRUE.equals(request.completed());
    }
}
