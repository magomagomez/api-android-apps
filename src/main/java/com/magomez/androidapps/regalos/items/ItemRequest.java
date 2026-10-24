package com.magomez.androidapps.regalos.items;

/** Body of POST and PUT /me/items; only title is required. */
public record ItemRequest(String title, String url, String store, Integer priceCents, String imageUrl,
                          String notes, Boolean wantedALot) {
}
