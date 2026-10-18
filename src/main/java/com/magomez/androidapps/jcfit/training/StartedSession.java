package com.magomez.androidapps.jcfit.training;

/** A session and whether POST /sessions has just created it (201) or it already existed (200). */
public record StartedSession(SessionReply session, boolean created) {
}
