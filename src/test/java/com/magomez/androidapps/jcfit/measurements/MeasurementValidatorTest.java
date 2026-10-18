package com.magomez.androidapps.jcfit.measurements;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Body of POST and PUT /measurements: ranges with their edges, decimals, at least one value, no future. */
class MeasurementValidatorTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    static MeasurementRequest weight(String kg) {
        return new MeasurementRequest(TODAY, new BigDecimal(kg), null, null, null, null, null, null);
    }

    private static MeasurementRequest bodyFat(String pct) {
        return new MeasurementRequest(TODAY, null, new BigDecimal(pct), null, null, null, null, null);
    }

    /** Every perimeter with the same value, to check the four cm fields at once. */
    private static MeasurementRequest[] perimeters(String cm) {
        BigDecimal value = new BigDecimal(cm);
        return new MeasurementRequest[]{
                new MeasurementRequest(TODAY, null, null, value, null, null, null, null),
                new MeasurementRequest(TODAY, null, null, null, value, null, null, null),
                new MeasurementRequest(TODAY, null, null, null, null, value, null, null),
                new MeasurementRequest(TODAY, null, null, null, null, null, value, null),
        };
    }

    private static MeasurementRequest check(MeasurementRequest request) {
        return MeasurementValidator.measurement(request, TODAY);
    }

    private static ApiException assertBadRequest(ThrowingCallable call, String code) {
        ApiException[] caught = new ApiException[1];
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(e.getCode()).isEqualTo(code);
            caught[0] = e;
        });
        return caught[0];
    }

    @Test
    void weightGoesFrom20To400Kg() {
        check(weight("20"));
        check(weight("400"));
        check(weight("78.45"));
        assertBadRequest(() -> check(weight("19.99")), "bad_request");
        assertBadRequest(() -> check(weight("400.01")), "bad_request");
    }

    @Test
    void bodyFatGoesFrom2To70Pct() {
        check(bodyFat("2"));
        check(bodyFat("70"));
        assertBadRequest(() -> check(bodyFat("1.9")), "bad_request");
        assertBadRequest(() -> check(bodyFat("70.1")), "bad_request");
    }

    @Test
    void everyPerimeterGoesFrom10To300Cm() {
        for (MeasurementRequest request : perimeters("10")) {
            check(request);
        }
        for (MeasurementRequest request : perimeters("300")) {
            check(request);
        }
        for (MeasurementRequest request : perimeters("9.9")) {
            assertBadRequest(() -> check(request), "bad_request");
        }
        for (MeasurementRequest request : perimeters("300.1")) {
            assertBadRequest(() -> check(request), "bad_request");
        }
    }

    @Test
    void tooManyDecimalsAreRejectedNotRounded() {
        assertThat(assertBadRequest(() -> check(weight("78.455")), "bad_request").getMessage())
                .contains("2 decimales");
        assertBadRequest(() -> check(bodyFat("17.85")), "bad_request");
        for (MeasurementRequest request : perimeters("84.25")) {
            assertBadRequest(() -> check(request), "bad_request");
        }
    }

    @Test
    void trailingZerosAreNotExtraDecimals() {
        assertThat(check(weight("78.400")).weightKg()).isEqualByComparingTo("78.4");
        for (MeasurementRequest request : perimeters("84.50")) {
            check(request);
        }
    }

    @Test
    void atLeastOneNumberIsRequired() {
        MeasurementRequest onlyNotes = new MeasurementRequest(TODAY, null, null, null, null, null, null, "Sin báscula");

        ApiException error = assertBadRequest(() -> check(onlyNotes), "bad_request");

        assertThat(error.getMessage()).isEqualTo("Rellena al menos una medida");
    }

    @Test
    void dateIsRequiredAndNotInTheFuture() {
        MeasurementRequest noDate = new MeasurementRequest(null, new BigDecimal("78"), null, null, null, null, null, null);
        MeasurementRequest tomorrow = new MeasurementRequest(TODAY.plusDays(1), new BigDecimal("78"), null, null, null,
                null, null, null);
        MeasurementRequest yesterday = new MeasurementRequest(TODAY.minusDays(1), new BigDecimal("78"), null, null, null,
                null, null, null);

        assertBadRequest(() -> check(noDate), "bad_request");
        assertBadRequest(() -> check(tomorrow), "future_date");
        assertThat(check(yesterday).measuredOn()).isEqualTo(TODAY.minusDays(1));
        assertThat(check(weight("78")).measuredOn()).isEqualTo(TODAY);
    }

    @Test
    void notesAreTrimmedBlankAsNullAndAtMost1000Characters() {
        MeasurementRequest padded = new MeasurementRequest(TODAY, new BigDecimal("78"), null, null, null, null, null,
                "  En ayunas ");
        MeasurementRequest blank = new MeasurementRequest(TODAY, new BigDecimal("78"), null, null, null, null, null, "   ");
        MeasurementRequest longest = new MeasurementRequest(TODAY, new BigDecimal("78"), null, null, null, null, null,
                "x".repeat(1000));
        MeasurementRequest tooLong = new MeasurementRequest(TODAY, new BigDecimal("78"), null, null, null, null, null,
                "x".repeat(1001));

        assertThat(check(padded).notes()).isEqualTo("En ayunas");
        assertThat(check(blank).notes()).isNull();
        assertThat(check(longest).notes()).hasSize(1000);
        assertBadRequest(() -> check(tooLong), "bad_request");
    }

    @Test
    void missingBodyIsABadRequest() {
        assertBadRequest(() -> check(null), "bad_request");
    }
}
