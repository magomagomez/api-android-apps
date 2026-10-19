package com.magomez.androidapps.jcfit.notifications;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/** A notice sent from the backoffice, with how many devices got it and how many did not. */
public record AdminNotification(
        int id,
        String title,
        String body,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant sentAt,
        int delivered,
        int failed
) {
}
