package com.magomez.androidapps.escapersthings.v2.rooms;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/** A room as the lists show it; played, playedOn and myGrade are those of the token's user. */
public record RoomSummary(
        int id,
        String name,
        String category,
        String city,
        List<Integer> playerIds,
        boolean played,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate playedOn,
        Averages averages,
        int gradeCount,
        Grade myGrade,
        PhotoRef poster,
        PhotoRef groupPhoto
) {
}
