package com.magomez.androidapps.jcfit.training;

/** Status of one day of the week in GET /weeks/{date}. */
public enum DayStatus {
    /** That weekday has no training in the program. */
    REST,
    /** Its session is finished. */
    DONE,
    /** Today and not done; it may be started. */
    TODAY,
    /** A past day that was not done. */
    MISSED,
    /** A future day. */
    PENDING
}
