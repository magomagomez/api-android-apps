package com.magomez.androidapps.regalos.linkpreview;

/** What a shop page proposes for a new wish; any field may be null. The app only fills empty fields with it. */
public record LinkPreview(String title, String imageUrl, Integer priceCents, String store) {
}
