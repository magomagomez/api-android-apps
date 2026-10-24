package com.magomez.androidapps.regalos.items;

/** An {@link ItemRequest} that passed {@link ItemRules}: trimmed, blanks as null, store filled in. */
record ValidItem(String title, String url, String store, Integer priceCents, String imageUrl, String notes,
                 boolean wantedALot) {
}
