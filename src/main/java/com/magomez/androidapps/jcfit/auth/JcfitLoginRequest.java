package com.magomez.androidapps.jcfit.auth;

/** Body of POST /auth/login. */
public record JcfitLoginRequest(String username, String password) {
}
