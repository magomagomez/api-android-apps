package com.magomez.androidapps.escapersthings.config;

public class ApiConfig {

    public static final String API_VERSION = "v1";
    public static final String BASE_URL = "/escapers-api/" + API_VERSION;

    private ApiConfig(){
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }
}
