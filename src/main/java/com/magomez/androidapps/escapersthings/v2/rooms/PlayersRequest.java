package com.magomez.androidapps.escapersthings.v2.rooms;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/** Body of PUT /escape-rooms/{id}/players: who played it and when (same date for all). */
public record PlayersRequest(
        List<Integer> userIds,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate playedOn
) {
}
