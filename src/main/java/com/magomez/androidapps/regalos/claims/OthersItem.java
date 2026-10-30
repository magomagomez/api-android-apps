package com.magomez.androidapps.regalos.claims;

/**
 * A wish of someone else or of a shared list; claim is null while nobody has claimed it.
 * addedBy is who wrote it into a shared list (null on a person's letter).
 */
public record OthersItem(long id, String title, String url, String store, Integer priceCents, String imageUrl,
                         String notes, boolean wantedALot, String addedBy, Claim claim) {
}
