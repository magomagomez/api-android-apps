package com.magomez.androidapps.regalos.claims;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/** One of my gifts; notice is null while nothing changed. photoVersion: see OwnItem. */
public record MyClaim(long itemId, String title, String url, String store, Integer priceCents, String imageUrl,
                      String notes, ClaimState state, Giver giver, boolean mine, Notice notice,
                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant photoVersion) {
}
