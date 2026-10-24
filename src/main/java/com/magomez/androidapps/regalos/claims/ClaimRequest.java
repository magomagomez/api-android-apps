package com.magomez.androidapps.regalos.claims;

/** Body of PUT /items/{id}/claim. */
public record ClaimRequest(ClaimState state, GiverRequest giver) {
}
