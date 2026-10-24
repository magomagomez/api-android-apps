package com.magomez.androidapps.regalos.config;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/** An expected regalos-api failure: {@link RegalosApiExceptionHandler} turns it into a {@link RegalosError}. */
public class RegalosException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String NOT_FOUND = "not_found";
    private static final String BAD_REQUEST = "bad_request";
    private static final String FORBIDDEN = "forbidden";

    private final HttpStatus status;
    private final String code;

    public RegalosException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /** 404 with the given Spanish message, e.g. "El deseo no existe". */
    public static RegalosException notFound(String message) {
        return new RegalosException(HttpStatus.NOT_FOUND, NOT_FOUND, message);
    }

    /** 400 for a field that fails validation; the message says which and why. */
    public static RegalosException badRequest(String message) {
        return new RegalosException(HttpStatus.BAD_REQUEST, BAD_REQUEST, message);
    }

    /** 403 for an action that is not the caller's to take (their own list, someone else's claim). */
    public static RegalosException forbidden(String message) {
        return new RegalosException(HttpStatus.FORBIDDEN, FORBIDDEN, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
