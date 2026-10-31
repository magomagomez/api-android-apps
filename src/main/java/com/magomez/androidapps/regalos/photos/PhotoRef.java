package com.magomez.androidapps.regalos.photos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/** The answer to a new photo: its version, the same value the wish or idea now carries. */
public record PhotoRef(@JsonFormat(shape = JsonFormat.Shape.STRING) Instant photoVersion) {
}
