package com.magomez.androidapps.jcfit.training;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/** Answer of POST /sessions and POST /sessions/{id}/finish; instants in ISO-8601 UTC. */
public record SessionReply(
        int sessionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant startedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant finishedAt
) {
}
