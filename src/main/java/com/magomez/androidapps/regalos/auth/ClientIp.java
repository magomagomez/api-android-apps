package com.magomez.androidapps.regalos.auth;

import jakarta.servlet.http.HttpServletRequest;

/** The caller's IP for login throttling. */
final class ClientIp {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private ClientIp() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    /**
     * Heroku's router appends the address it saw to X-Forwarded-For, so the last entry is the
     * one a client cannot forge; anything before it is whatever the client sent.
     */
    static String of(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
