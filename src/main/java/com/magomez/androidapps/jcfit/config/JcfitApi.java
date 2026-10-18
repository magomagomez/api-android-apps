package com.magomez.androidapps.jcfit.config;

/** Routes of jcfit-api (contract: jcfit-android/tasks/contract.md). */
public final class JcfitApi {

    public static final String API_VERSION = "v1";
    public static final String BASE_URL = "/jcfit-api/" + API_VERSION;
    public static final String ADMIN_URL = BASE_URL + "/admin";

    private JcfitApi() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }
}
