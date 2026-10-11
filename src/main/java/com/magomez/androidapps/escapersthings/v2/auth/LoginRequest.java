package com.magomez.androidapps.escapersthings.v2.auth;

/** Body of POST /auth/login. */
public record LoginRequest(String username, String password) {
}
