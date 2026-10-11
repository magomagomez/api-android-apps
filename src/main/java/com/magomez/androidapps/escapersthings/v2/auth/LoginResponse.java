package com.magomez.androidapps.escapersthings.v2.auth;

/** The token goes back once; the database only keeps its SHA-256. */
public record LoginResponse(String token, UserDTO user) {
}
