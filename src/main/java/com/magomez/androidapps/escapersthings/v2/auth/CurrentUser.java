package com.magomez.androidapps.escapersthings.v2.auth;

/**
 * The user behind the Bearer token of the request, put there by {@link BearerTokenFilter}.
 * tokenHash identifies the session itself, for logout and for closing the other sessions.
 */
public record CurrentUser(int id, String name, String tokenHash) {

    /** Request attribute that holds it; controllers read it with {@code @RequestAttribute}. */
    public static final String ATTRIBUTE = "escapers.v2.currentUser";

    public UserDTO toDto() {
        return new UserDTO(id, name);
    }
}
