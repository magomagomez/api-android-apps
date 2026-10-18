package com.magomez.androidapps.jcfit.config;

/** Body of every jcfit-api error: a stable code for the app and a message in Spanish. */
public record ApiError(String error, String message) {
}
