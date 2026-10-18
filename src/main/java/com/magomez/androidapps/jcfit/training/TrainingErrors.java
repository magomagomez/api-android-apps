package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.springframework.http.HttpStatus;

/** The training errors of the contract, with their codes and Spanish messages. */
final class TrainingErrors {

    private static final String BEFORE_PROGRAM_START = "before_program_start";
    private static final String NO_ACTIVE_PROGRAM = "no_active_program";
    private static final String FUTURE_DATE = "future_date";

    private TrainingErrors() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static ApiException beforeProgramStart() {
        return new ApiException(HttpStatus.BAD_REQUEST, BEFORE_PROGRAM_START,
                "Esa semana es anterior al inicio del programa");
    }

    static ApiException noActiveProgram() {
        return new ApiException(HttpStatus.NOT_FOUND, NO_ACTIVE_PROGRAM, "Todavía no hay un programa activo");
    }

    static ApiException futureDate() {
        return new ApiException(HttpStatus.BAD_REQUEST, FUTURE_DATE, "No puedes empezar un entreno de un día futuro");
    }

    static ApiException dayNotFound() {
        return ApiException.notFound("El entreno no existe");
    }

    static ApiException sessionNotFound() {
        return ApiException.notFound("La sesión no existe");
    }

    static ApiException exerciseNotFound() {
        return ApiException.notFound("El ejercicio no existe");
    }

    static ApiException dateNotOnDay() {
        return ApiException.badRequest("La fecha no corresponde a ese día del programa");
    }

    static ApiException exerciseNotInDay() {
        return ApiException.badRequest("Ese ejercicio no está en el entreno de la sesión");
    }
}
