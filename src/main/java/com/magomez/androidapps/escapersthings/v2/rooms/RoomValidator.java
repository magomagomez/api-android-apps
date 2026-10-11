package com.magomez.androidapps.escapersthings.v2.rooms;

import com.magomez.androidapps.escapersthings.v2.config.ApiException;
import org.springframework.http.HttpStatus;

/** Checks POST and PUT /escape-rooms and returns the body trimmed, with a blank city as null. */
final class RoomValidator {

    static final int MAX_NAME = 120;
    static final int MAX_CITY = 80;
    private static final String INVALID_ROOM = "invalid_room";

    private RoomValidator() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static SaveRoomRequest room(SaveRoomRequest request) {
        if (request == null) {
            throw invalid("Faltan los datos de la sala");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw invalid("El nombre es obligatorio");
        }
        if (name.length() > MAX_NAME) {
            throw invalid("El nombre no puede pasar de " + MAX_NAME + " caracteres");
        }
        String category = request.category() == null ? null : request.category().trim();
        if (category == null || !RoomCategories.ALL.contains(category)) {
            throw invalid("La categoría debe ser una de: " + String.join(", ", RoomCategories.ALL));
        }
        String city = request.city() == null || request.city().isBlank() ? null : request.city().trim();
        if (city != null && city.length() > MAX_CITY) {
            throw invalid("La ciudad no puede pasar de " + MAX_CITY + " caracteres");
        }
        return new SaveRoomRequest(name, category, city);
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, INVALID_ROOM, message);
    }
}
