package com.magomez.androidapps.escapersthings.v2.photos;

/** A row of escape_room_photos; etag is the SHA-256 in hex of data. */
public record StoredPhoto(String contentType, byte[] data, String etag) {
}
