package com.magomez.androidapps.jctravels.cities.dto;

/** What a city has content for; each maps to its flag column on cities (never request input). */
public enum CityContent {
    MONUMENTS("has_monuments"),
    PARKS("has_parks"),
    OUTLETS("has_outlets");

    private final String column;

    CityContent(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }
}
