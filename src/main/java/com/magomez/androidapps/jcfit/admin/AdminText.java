package com.magomez.androidapps.jcfit.admin;

import com.magomez.androidapps.jcfit.config.ApiException;

/** Text fields of the backoffice: trimmed, blank stored as null, lengths in characters like Postgres. */
final class AdminText {

    private AdminText() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /** Trimmed, or null when blank; 400 with the message when longer than max. */
    static String optional(String value, int max, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (length(trimmed) > max) {
            throw ApiException.badRequest(message);
        }
        return trimmed;
    }

    /** Trimmed; 400 with the message when blank or longer than max. */
    static String required(String value, int max, String message) {
        String trimmed = optional(value, max, message);
        if (trimmed == null) {
            throw ApiException.badRequest(message);
        }
        return trimmed;
    }

    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }
}
