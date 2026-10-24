package com.magomez.androidapps.regalos.claims;

/** A claim as the other givers see it (R3). mine: I give it. canEdit: I may change or release it (R4). */
public record Claim(ClaimState state, Giver giver, boolean mine, boolean canEdit) {
}
