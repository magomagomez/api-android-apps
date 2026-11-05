package com.magomez.androidapps.regalos.ideas;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

/**
 * An idea of what to give someone. Its author sees it, and so do the people it is shared with;
 * never the person it is for (R7, R13). mine: I wrote it. sharedWith: only sent to its author,
 * null to anyone else. bought: its author bought it. claim: who of the people it is shared with
 * gives it, or null. photoVersion: see OwnItem.
 */
public record Idea(long id, int forUserId, String title, String url, String store, Integer priceCents, String notes,
                   boolean bought, @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
                   @JsonFormat(shape = JsonFormat.Shape.STRING) Instant photoVersion,
                   IdeaAuthor author, boolean mine, List<Integer> sharedWith, IdeaClaim claim) {

    Idea withSharedWith(List<Integer> people) {
        return new Idea(id, forUserId, title, url, store, priceCents, notes, bought, createdAt, photoVersion, author,
                mine, people, claim);
    }
}
