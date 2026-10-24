package com.magomez.androidapps.regalos.items;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/** One of my own wishes. It has no claim field on purpose: the owner never sees who gives it (R1). */
public record OwnItem(long id, String title, String url, String store, Integer priceCents, String imageUrl,
                      String notes, boolean wantedALot, ItemStatus status,
                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant updatedAt) {
}
