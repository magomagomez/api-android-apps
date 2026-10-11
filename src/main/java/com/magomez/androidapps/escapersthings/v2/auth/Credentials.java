package com.magomez.androidapps.escapersthings.v2.auth;

/** A users row as login needs it. passwordHash is null until {@link PasswordHashBackfill} runs. */
record Credentials(int id, String name, String passwordHash) {

    UserDTO toDto() {
        return new UserDTO(id, name);
    }
}
