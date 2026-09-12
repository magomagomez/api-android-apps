package com.magomez.androidapps.movierec.scoring.letterboxd;

/**
 * Raised when a Letterboxd ratings / diary CSV cannot be parsed: missing header,
 * missing required column, or an unusable value on a row. The message names the line.
 */
public class LetterboxdCsvException extends RuntimeException {

    public LetterboxdCsvException(String message) {
        super(message);
    }
}
