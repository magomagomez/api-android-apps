package com.magomez.androidapps.escapersthings.v2.rooms;

import java.util.List;

/** The closed list of room categories (escape_rooms.type). */
public final class RoomCategories {

    public static final List<String> ALL = List.of(
            "Aventura", "Terror", "Cinéfilo", "Tensión", "Histórico", "Hall Escape", "Battle Royale");

    private RoomCategories() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }
}
