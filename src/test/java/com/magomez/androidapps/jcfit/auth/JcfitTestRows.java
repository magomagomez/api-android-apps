package com.magomez.androidapps.jcfit.auth;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fake result rows (unlike Map.of they accept SQL NULL) and the assertion for an ApiException. */
public final class JcfitTestRows {

    private JcfitTestRows() {
    }

    public static Map<String, Object> row(Object... columnsAndValues) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < columnsAndValues.length; i += 2) {
            row.put((String) columnsAndValues[i], columnsAndValues[i + 1]);
        }
        return row;
    }

    public static ApiException assertApiError(ThrowingCallable call, HttpStatus status, String code) {
        ApiException[] caught = new ApiException[1];
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            assertThat(e.getCode()).isEqualTo(code);
            caught[0] = e;
        });
        return caught[0];
    }
}
