package com.magomez.androidapps.regalos.config;

import java.time.Instant;
import java.time.OffsetDateTime;

/** Small conversions shared by the regalos row mappers. */
public final class RegalosColumns {

    private RegalosColumns() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /** A nullable TIMESTAMPTZ column as an Instant, e.g. photo_updated_at of a wish without photo. */
    public static Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
