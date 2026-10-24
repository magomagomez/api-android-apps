package com.magomez.androidapps.regalos.auth;

import com.magomez.androidapps.regalos.people.Me;

/** The token goes back once; the database only keeps its SHA-256. */
public record AuthResponse(String token, Me user) {
}
