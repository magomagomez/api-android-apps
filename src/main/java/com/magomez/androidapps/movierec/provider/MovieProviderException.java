package com.magomez.androidapps.movierec.provider;

/** Raised when an external movie data source fails. */
public class MovieProviderException extends Exception {

    public MovieProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public MovieProviderException(String message) {
        super(message);
    }
}
