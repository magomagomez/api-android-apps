package com.magomez.androidapps.escapersthings.v2.auth;

/** Body of PUT /me/password. */
public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
