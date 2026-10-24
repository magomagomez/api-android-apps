package com.magomez.androidapps.regalos.config;

/** Body of every regalos-api error: a stable code for the app and a message in Spanish. */
public record RegalosError(String error, String message) {
}
