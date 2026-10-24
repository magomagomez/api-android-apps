package com.magomez.androidapps.regalos.people;

import com.magomez.androidapps.regalos.config.RegalosException;

/** Validation of the profile fields shared by the registration and PUT /me. */
public final class ProfileRules {

    public static final int MAX_DISPLAY_NAME = 40;

    private ProfileRules() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /** Trimmed, between 1 and {@link #MAX_DISPLAY_NAME} characters. */
    public static String displayName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.length() > MAX_DISPLAY_NAME) {
            throw RegalosException.badRequest("El nombre debe tener entre 1 y " + MAX_DISPLAY_NAME + " caracteres");
        }
        return name;
    }
}
