package com.magomez.androidapps.regalos.items;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * One of my own wishes. It has no claim field on purpose: the owner never sees who gives it (R1).
 * photoVersion is when its photo last changed, or null without one.
 */
public record OwnItem(long id, String title, String url, String store, Integer priceCents, String imageUrl,
                      String notes, boolean wantedALot, ItemStatus status,
                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant updatedAt,
                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant photoVersion) {
}
