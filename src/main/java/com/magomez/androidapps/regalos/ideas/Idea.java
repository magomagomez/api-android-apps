package com.magomez.androidapps.regalos.ideas;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/** One of my ideas of what to give someone. Only I, its author, ever see it (R7). photoVersion: see OwnItem. */
public record Idea(long id, int forUserId, String title, String url, String store, Integer priceCents, String notes,
                   boolean bought, @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
                   @JsonFormat(shape = JsonFormat.Shape.STRING) Instant photoVersion) {
}
