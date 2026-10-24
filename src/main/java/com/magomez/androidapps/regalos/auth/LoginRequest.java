package com.magomez.androidapps.regalos.auth;

/** Body of POST /auth/login. */
public record LoginRequest(String username, String password) {
}
