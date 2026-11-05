package com.magomez.androidapps.regalos.ideas;

import com.magomez.androidapps.regalos.claims.ClaimState;
import com.magomez.androidapps.regalos.claims.Giver;

/** Who of the people an idea is shared with gives it; mine: I do. */
public record IdeaClaim(ClaimState state, Giver giver, boolean mine) {
}
