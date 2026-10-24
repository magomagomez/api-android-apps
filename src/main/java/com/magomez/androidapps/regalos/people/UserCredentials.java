package com.magomez.androidapps.regalos.people;

/** A regalos_users row as the login needs it. */
public record UserCredentials(int id, String passwordHash) {
}
