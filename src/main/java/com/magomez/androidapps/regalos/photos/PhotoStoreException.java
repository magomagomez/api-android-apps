package com.magomez.androidapps.regalos.photos;

import java.io.Serial;

/** The photo store could not be reached or refused the request. The message never holds a secret. */
public class PhotoStoreException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PhotoStoreException(String message) {
        super(message);
    }
}
