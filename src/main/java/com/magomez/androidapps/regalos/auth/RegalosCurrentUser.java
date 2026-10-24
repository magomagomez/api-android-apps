package com.magomez.androidapps.regalos.auth;

/**
 * The user behind the Bearer token of the request, put there by {@link RegalosBearerTokenFilter}.
 * tokenHash identifies the session itself, for logout.
 */
public record RegalosCurrentUser(int id, String displayName, String tokenHash) {

    /** Request attribute that holds it; controllers read it with {@code @RequestAttribute}. */
    public static final String ATTRIBUTE = "regalos.currentUser";
}
