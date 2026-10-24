package com.magomez.androidapps.regalos.people;

/** Body of PUT /me; a null birthday removes it. */
public record ProfileRequest(String displayName, Birthday birthday) {
}
