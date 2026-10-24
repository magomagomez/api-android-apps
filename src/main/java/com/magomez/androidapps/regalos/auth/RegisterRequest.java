package com.magomez.androidapps.regalos.auth;

import com.magomez.androidapps.regalos.people.Birthday;

/** Body of POST /auth/register; birthday is optional. */
public record RegisterRequest(String inviteCode, String displayName, String username, String password,
                              Birthday birthday) {
}
