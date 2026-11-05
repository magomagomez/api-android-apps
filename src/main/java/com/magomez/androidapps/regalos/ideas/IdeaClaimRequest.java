package com.magomez.androidapps.regalos.ideas;

import com.magomez.androidapps.regalos.claims.ClaimState;

/** Body of PUT /ideas/{id}/claim: «La regalo jo» (RESERVED) or «Ja la tinc» (BOUGHT). */
public record IdeaClaimRequest(ClaimState state) {
}
