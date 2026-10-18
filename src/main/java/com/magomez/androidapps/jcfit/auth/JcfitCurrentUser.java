package com.magomez.androidapps.jcfit.auth;

/**
 * The user behind the Bearer token of the request, put there by {@link JcfitBearerTokenFilter}.
 * tokenHash identifies the session itself, for logout.
 */
public record JcfitCurrentUser(int id, String name, String tokenHash) {

    /** Request attribute that holds it; controllers read it with {@code @RequestAttribute}. */
    public static final String ATTRIBUTE = "jcfit.currentUser";
}
