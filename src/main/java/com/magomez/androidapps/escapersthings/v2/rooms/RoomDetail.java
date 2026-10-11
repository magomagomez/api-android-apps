package com.magomez.androidapps.escapersthings.v2.rooms;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

/** RoomSummary plus everyone's grades, by user id: the summary fields are inlined in the JSON. */
public record RoomDetail(@JsonUnwrapped RoomSummary summary, List<UserGrade> grades) {
}
