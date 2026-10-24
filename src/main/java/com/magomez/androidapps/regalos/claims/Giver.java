package com.magomez.androidapps.regalos.claims;

/** userId is null when the giver is someone outside the app. */
public record Giver(Integer userId, String name) {
}
