package com.magomez.androidapps.escapersthings.v2.config;

public final class ApiConfig {

    public static final String API_VERSION = "v2";
    public static final String BASE_URL = "/escapers-api/" + API_VERSION;

    private ApiConfig() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }
}
