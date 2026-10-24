package com.magomez.androidapps.regalos.claims;

/** One of my gifts; notice is null while nothing changed. */
public record MyClaim(long itemId, String title, String url, String store, Integer priceCents, String imageUrl,
                      String notes, ClaimState state, Giver giver, boolean mine, Notice notice) {
}
