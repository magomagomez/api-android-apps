package com.magomez.androidapps.regalos.config;

/** Routes of regalos-api (contract: regalos-android/tasks/contract.md). */
public final class RegalosApi {

    public static final String API_VERSION = "v1";
    public static final String BASE_URL = "/regalos-api/" + API_VERSION;

    private RegalosApi() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }
}
