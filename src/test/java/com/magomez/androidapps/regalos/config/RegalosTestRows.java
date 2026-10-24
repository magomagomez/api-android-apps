package com.magomez.androidapps.regalos.config;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fake result rows (unlike Map.of they accept SQL NULL) and the assertion for a RegalosException. */
public final class RegalosTestRows {

    private RegalosTestRows() {
    }

    public static Map<String, Object> row(Object... columnsAndValues) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < columnsAndValues.length; i += 2) {
            row.put((String) columnsAndValues[i], columnsAndValues[i + 1]);
        }
        return row;
    }

    public static RegalosException assertRegalosError(ThrowingCallable call, HttpStatus status, String code) {
        RegalosException[] caught = new RegalosException[1];
        assertThatThrownBy(call).isInstanceOfSatisfying(RegalosException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            assertThat(e.getCode()).isEqualTo(code);
            caught[0] = e;
        });
        return caught[0];
    }
}
