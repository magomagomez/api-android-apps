package com.magomez.androidapps.regalos.claims;

/** userId goes with USER and name with EXTERNAL. */
public record GiverRequest(GiverType type, Integer userId, String name) {
}
