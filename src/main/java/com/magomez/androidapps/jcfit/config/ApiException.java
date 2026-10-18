package com.magomez.androidapps.jcfit.config;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/** An expected jcfit-api failure: {@link JcfitApiExceptionHandler} turns it into an {@link ApiError}. */
public class ApiException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String NOT_FOUND = "not_found";
    private static final String BAD_REQUEST = "bad_request";

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /** 404 with the given Spanish message, e.g. "El ejercicio no existe". */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND, message);
    }

    /** 400 for a field that fails validation; the message says which and why. */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, BAD_REQUEST, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
