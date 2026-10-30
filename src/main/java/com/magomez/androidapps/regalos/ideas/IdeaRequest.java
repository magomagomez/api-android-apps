package com.magomez.androidapps.regalos.ideas;

/** Body of POST /people/{id}/ideas and PUT /ideas/{id}; PUT replaces the whole idea. */
public record IdeaRequest(String title, String url, String store, Integer priceCents, String notes, Boolean bought) {
}
