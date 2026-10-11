package com.magomez.androidapps.escapersthings.v2.config;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/** An expected v2 failure: {@link ApiExceptionHandler} turns it into an {@link ApiError}. */
public class ApiException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", "La sala no existe");
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
