package com.magomez.androidapps.regalos.people;

/** The logged-in user as GET /me returns it; birthday may be null. */
public record Me(int id, String username, String displayName, Birthday birthday) {
}
