package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.config.ApiException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class ApiAssertions {

    private ApiAssertions() {
    }

    static ApiException assertApiError(ThrowingCallable call, HttpStatus status, String code) {
        ApiException[] caught = new ApiException[1];
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            assertThat(e.getCode()).isEqualTo(code);
            caught[0] = e;
        });
        return caught[0];
    }
}
