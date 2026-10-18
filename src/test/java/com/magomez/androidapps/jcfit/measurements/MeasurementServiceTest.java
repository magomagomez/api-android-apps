package com.magomez.androidapps.jcfit.measurements;

import com.magomez.androidapps.jcfit.config.ApiException;
import com.magomez.androidapps.jcfit.config.JcfitConfig;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static com.magomez.androidapps.jcfit.measurements.MeasurementFakeJdbcTemplate.row;
import static com.magomez.androidapps.jcfit.measurements.MeasurementValidatorTest.weight;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reading the history and writing measurements through the DAO, without a database. */
class MeasurementServiceTest {

    private static final String ALL = "FROM jcfit_measurements ORDER BY";
    private static final String BY_ID = "FROM jcfit_measurements WHERE id = ?";
    private static final String ALL_PHOTOS = "FROM jcfit_photos ORDER BY";
    private static final String PHOTOS_OF_ONE = "FROM jcfit_photos WHERE measurement_id = ?";
    /** Still the 17th in UTC, but already 00:30 on the 18th in Madrid. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T22:30:00Z"), JcfitConfig.SPAIN);

    private MeasurementFakeJdbcTemplate jdbc;
    private MeasurementService service;

    @BeforeEach
    void setUp() {
        jdbc = new MeasurementFakeJdbcTemplate();
        service = new MeasurementService(new MeasurementDao(jdbc), CLOCK);
    }

    static Map<String, Object> measurement(int id, String measuredOn, String weightKg) {
        return row("id", id, "measured_on", LocalDate.parse(measuredOn),
                "weight_kg", weightKg == null ? null : new BigDecimal(weightKg),
                "body_fat_pct", null, "chest_cm", null, "waist_cm", null, "hip_cm", null, "leg_cm", null,
                "notes", null);
    }

    static Map<String, Object> photo(int id, int measurementId, String pose) {
        return row("id", id, "measurement_id", measurementId, "pose", pose);
    }

    private static void assertApiError(ThrowingCallable call, HttpStatus status, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            assertThat(e.getCode()).isEqualTo(code);
        });
    }

    @Test
    void emptyHistoryHasNoFirstNorLatest() {
        MeasurementHistory history = service.history();

        assertThat(history.first()).isNull();
        assertThat(history.latest()).isNull();
        assertThat(history.items()).isEmpty();
    }

    @Test
    void oneMeasurementIsBothFirstAndLatest() {
        jdbc.rows(ALL, measurement(4, "2026-10-01", "80.00"));

        MeasurementHistory history = service.history();

        assertThat(history.items()).hasSize(1);
        assertThat(history.first()).isEqualTo(history.items().get(0));
        assertThat(history.latest()).isEqualTo(history.items().get(0));
    }

    @Test
    void itemsGoNewestFirstWithTheOldestAsFirst() {
        jdbc.rows(ALL, measurement(9, "2026-10-13", "78.40"), measurement(5, "2026-10-06", "79.10"),
                measurement(2, "2026-09-29", "80.00"));

        MeasurementHistory history = service.history();

        assertThat(jdbc.calls.get(0).sql()).endsWith("ORDER BY measured_on DESC");
        assertThat(history.items()).extracting(Measurement::id).containsExactly(9, 5, 2);
        assertThat(history.latest().id()).isEqualTo(9);
        assertThat(history.first().id()).isEqualTo(2);
    }

    @Test
    void photosAreGroupedByMeasurementFrontSideBackInOneQuery() {
        jdbc.rows(ALL, measurement(9, "2026-10-13", "78.40"), measurement(5, "2026-10-06", "79.10"),
                        measurement(2, "2026-09-29", "80.00"))
                .rows(ALL_PHOTOS, photo(31, 5, "BACK"), photo(30, 5, "FRONT"), photo(40, 9, "SIDE"),
                        photo(32, 5, "SIDE"), photo(41, 9, "FRONT"));

        MeasurementHistory history = service.history();

        assertThat(jdbc.callsWith("jcfit_photos")).hasSize(1);
        assertThat(history.items().get(0).photos()).containsExactly(
                new MeasurementPhoto(41, MeasurementPose.FRONT), new MeasurementPhoto(40, MeasurementPose.SIDE));
        assertThat(history.items().get(1).photos()).extracting(MeasurementPhoto::pose)
                .containsExactly(MeasurementPose.FRONT, MeasurementPose.SIDE, MeasurementPose.BACK);
        assertThat(history.items().get(2).photos()).isEmpty();
    }

    @Test
    void numbersComeWithoutTrailingZeros() {
        jdbc.rows(ALL, row("id", 9, "measured_on", LocalDate.of(2026, 10, 13), "weight_kg", new BigDecimal("78.40"),
                "body_fat_pct", new BigDecimal("17.8"), "chest_cm", new BigDecimal("101.0"), "waist_cm", null,
                "hip_cm", new BigDecimal("100.0"), "leg_cm", null, "notes", "En ayunas"));

        Measurement measurement = service.history().latest();

        assertThat(measurement.weightKg()).hasToString("78.4");
        assertThat(measurement.bodyFatPct()).hasToString("17.8");
        assertThat(measurement.chestCm()).hasToString("101");
        assertThat(measurement.hipCm()).hasToString("100");
        assertThat(measurement.waistCm()).isNull();
        assertThat(measurement.notes()).isEqualTo("En ayunas");
    }

    @Test
    void createInsertsWithParametersAndReturnsTheStoredRow() {
        jdbc.scalar("INSERT INTO jcfit_measurements", 12).rows(BY_ID, measurement(12, "2026-10-18", "78.45"))
                .rows(PHOTOS_OF_ONE);

        Measurement created = service.create(new MeasurementRequest(LocalDate.of(2026, 10, 18), new BigDecimal("78.45"),
                null, new BigDecimal("101"), null, null, null, "  En ayunas "));

        MeasurementFakeJdbcTemplate.Call insert = jdbc.calls.get(0);
        assertThat(insert.sql()).startsWith("INSERT INTO jcfit_measurements").endsWith("RETURNING id");
        assertThat(insert.args()).containsExactly(LocalDate.of(2026, 10, 18), new BigDecimal("78.45"), null,
                new BigDecimal("101"), null, null, null, "En ayunas");
        assertThat(jdbc.callsWith(BY_ID).get(0).args()).containsExactly(12);
        assertThat(jdbc.callsWith(PHOTOS_OF_ONE).get(0).args()).containsExactly(12);
        assertThat(created.id()).isEqualTo(12);
        assertThat(created.photos()).isEmpty();
    }

    @Test
    void todayIsTheDayInMadrid() {
        jdbc.scalar("INSERT INTO jcfit_measurements", 12).rows(BY_ID, measurement(12, "2026-10-18", "78"));

        service.create(weight("78"));

        assertApiError(() -> service.create(new MeasurementRequest(LocalDate.of(2026, 10, 19), new BigDecimal("78"),
                null, null, null, null, null, null)), HttpStatus.BAD_REQUEST, "future_date");
    }

    @Test
    void invalidBodyNeverReachesTheDatabase() {
        assertApiError(() -> service.create(weight("19")), HttpStatus.BAD_REQUEST, "bad_request");
        assertApiError(() -> service.update(9, weight("19")), HttpStatus.BAD_REQUEST, "bad_request");

        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void secondMeasurementOnTheSameDayIsAConflict() {
        jdbc.fail("INSERT INTO jcfit_measurements", new DuplicateKeyException("measured_on"));

        assertThatThrownBy(() -> service.create(weight("78"))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.getCode()).isEqualTo("measurement_exists");
            assertThat(e.getMessage()).isEqualTo("Ya hay un registro ese día");
        });
    }

    @Test
    void updateWritesEveryFieldByIdAndReturnsTheRow() {
        jdbc.rows(BY_ID, measurement(9, "2026-10-13", "78.2"));

        Measurement updated = service.update(9, new MeasurementRequest(LocalDate.of(2026, 10, 13),
                new BigDecimal("78.2"), new BigDecimal("17.5"), null, null, null, new BigDecimal("58"), null));

        MeasurementFakeJdbcTemplate.Call update = jdbc.calls.get(0);
        assertThat(update.sql()).startsWith("UPDATE jcfit_measurements SET").endsWith("WHERE id = ?");
        assertThat(update.args()).containsExactly(LocalDate.of(2026, 10, 13), new BigDecimal("78.2"),
                new BigDecimal("17.5"), null, null, null, new BigDecimal("58"), null, 9);
        assertThat(updated.id()).isEqualTo(9);
    }

    @Test
    void updateOfAMissingMeasurementIsNotFound() {
        jdbc.updateCount("UPDATE jcfit_measurements", 0);

        assertApiError(() -> service.update(99, weight("78")), HttpStatus.NOT_FOUND, "not_found");
    }

    @Test
    void movingAMeasurementToAnOccupiedDayIsAConflict() {
        jdbc.fail("UPDATE jcfit_measurements", new DuplicateKeyException("measured_on"));

        assertApiError(() -> service.update(9, weight("78")), HttpStatus.CONFLICT, "measurement_exists");
    }

    @Test
    void deleteById() {
        service.delete(9);

        assertThat(jdbc.calls).hasSize(1);
        assertThat(jdbc.calls.get(0).sql()).isEqualTo("DELETE FROM jcfit_measurements WHERE id = ?");
        assertThat(jdbc.calls.get(0).args()).containsExactly(9);
    }

    @Test
    void deleteOfAMissingMeasurementIsNotFound() {
        jdbc.updateCount("DELETE FROM jcfit_measurements", 0);

        assertApiError(() -> service.delete(99), HttpStatus.NOT_FOUND, "not_found");
    }
}
