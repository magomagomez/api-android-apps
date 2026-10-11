package com.magomez.androidapps.escapersthings.v2.photos;

import java.util.Arrays;
import java.util.Optional;

/** The two photos of a room, as they appear in the URL and in escape_room_photos.kind. */
public enum PhotoKind {
    POSTER("poster"),
    GROUP("group");

    private final String value;

    PhotoKind(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    static Optional<PhotoKind> fromPath(String value) {
        return Arrays.stream(values()).filter(kind -> kind.value.equals(value)).findFirst();
    }
}
