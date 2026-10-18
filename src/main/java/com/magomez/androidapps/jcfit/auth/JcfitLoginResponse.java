package com.magomez.androidapps.jcfit.auth;

/** The token goes back once; the database only keeps its SHA-256. */
public record JcfitLoginResponse(String token) {
}
