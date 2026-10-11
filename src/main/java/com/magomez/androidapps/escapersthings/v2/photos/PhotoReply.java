package com.magomez.androidapps.escapersthings.v2.photos;

/** What GET answers: the photo, or just "not modified" when the client already has that etag. */
public record PhotoReply(StoredPhoto photo, boolean notModified) {
}
