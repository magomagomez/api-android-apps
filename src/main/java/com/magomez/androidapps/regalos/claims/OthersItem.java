package com.magomez.androidapps.regalos.claims;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * A wish of someone else or of a shared list; claim is null while nobody has claimed it.
 * addedBy is who wrote it into a shared list (null on a person's letter). photoVersion: see OwnItem.
 */
public record OthersItem(long id, String title, String url, String store, Integer priceCents, String imageUrl,
                         String notes, boolean wantedALot, String addedBy, Claim claim,
                         @JsonFormat(shape = JsonFormat.Shape.STRING) Instant photoVersion) {
}
