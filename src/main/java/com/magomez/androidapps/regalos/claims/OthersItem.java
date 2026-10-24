package com.magomez.androidapps.regalos.claims;

/** A wish of someone else; claim is null while nobody has claimed it. */
public record OthersItem(long id, String title, String url, String store, Integer priceCents, String imageUrl,
                         String notes, boolean wantedALot, Claim claim) {
}
