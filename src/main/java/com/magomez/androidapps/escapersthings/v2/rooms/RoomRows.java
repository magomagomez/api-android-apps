package com.magomez.androidapps.escapersthings.v2.rooms;

import java.time.LocalDate;

/** Rows read by {@link RoomDao}, before they are put together into a {@link RoomSummary}. */
final class RoomRows {

    private RoomRows() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /** escape_rooms: category is the type column and city the ubication one. */
    record Room(int id, String name, String category, String city) {
    }

    record Player(int roomId, int userId, LocalDate playedOn) {
    }

    record UserGradeRow(int roomId, int userId, String userName, Grade grade) {
    }

    record Photo(int roomId, String kind, String etag) {
    }
}
