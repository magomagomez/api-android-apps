package com.magomez.androidapps.escapersthings.v2.rooms;

/** Body of POST and PUT /escape-rooms. */
public record SaveRoomRequest(String name, String category, String city) {
}
