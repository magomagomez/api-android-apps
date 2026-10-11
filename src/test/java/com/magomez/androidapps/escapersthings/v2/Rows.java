package com.magomez.androidapps.escapersthings.v2;

import java.util.HashMap;
import java.util.Map;

/** Builds fake result rows; unlike Map.of it accepts null values (SQL NULL). */
final class Rows {

    private Rows() {
    }

    static Map<String, Object> row(Object... columnsAndValues) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < columnsAndValues.length; i += 2) {
            row.put((String) columnsAndValues[i], columnsAndValues[i + 1]);
        }
        return row;
    }
}
