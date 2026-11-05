package com.magomez.androidapps.regalos.ideas;

import java.util.List;

/**
 * Body of POST /people/{id}/ideas and PUT /ideas/{id}; PUT replaces the whole idea, sharedWith
 * included (null or empty: shared with nobody).
 */
public record IdeaRequest(String title, String url, String store, Integer priceCents, String notes, Boolean bought,
                          List<Integer> sharedWith) {
}
