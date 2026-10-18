package com.magomez.androidapps.jcfit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Errors of jcfit-api as {@code {"error", "message"}}, never with internal details. */
@RestControllerAdvice(basePackages = "com.magomez.androidapps.jcfit")
public class JcfitApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(JcfitApiExceptionHandler.class);

    static final ApiError BAD_REQUEST = new ApiError("bad_request", "La petición no es válida");
    static final ApiError SERVER_ERROR = new ApiError("server_error", "Error interno, inténtalo más tarde");

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handle(ApiException e) {
        return json(e.getStatus(), new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception e) {
        return json(HttpStatus.BAD_REQUEST, BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDatabase(DataAccessException e) {
        log.error("jcfit-api: database error", e);
        return json(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR);
    }

    /** Always JSON, whatever the client accepts (the photo GET is asked for image/*). */
    private static ResponseEntity<ApiError> json(HttpStatus status, ApiError error) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(error);
    }
}
