package com.magomez.androidapps.regalos.config;

import com.magomez.androidapps.regalos.photos.PhotoStoreException;
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

/** Errors of regalos-api as {@code {"error", "message"}}, never with internal details. */
@RestControllerAdvice(basePackages = "com.magomez.androidapps.regalos")
public class RegalosApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RegalosApiExceptionHandler.class);

    static final RegalosError BAD_REQUEST = new RegalosError("bad_request", "La petición no es válida");
    static final RegalosError SERVER_ERROR = new RegalosError("server_error", "Error interno, inténtalo más tarde");
    static final RegalosError PHOTO_STORE_ERROR = new RegalosError("photo_store_error",
            "No se ha podido guardar o leer la foto, inténtalo más tarde");

    @ExceptionHandler(RegalosException.class)
    public ResponseEntity<RegalosError> handle(RegalosException e) {
        return json(e.getStatus(), new RegalosError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<RegalosError> handleBadRequest(Exception e) {
        return json(HttpStatus.BAD_REQUEST, BAD_REQUEST);
    }

    /** Google Drive failed or refused: the photo store already logged why, without secrets. */
    @ExceptionHandler(PhotoStoreException.class)
    public ResponseEntity<RegalosError> handlePhotoStore(PhotoStoreException e) {
        return json(HttpStatus.BAD_GATEWAY, PHOTO_STORE_ERROR);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<RegalosError> handleDatabase(DataAccessException e) {
        log.error("regalos-api: database error", e);
        return json(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR);
    }

    private static ResponseEntity<RegalosError> json(HttpStatus status, RegalosError error) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(error);
    }
}
