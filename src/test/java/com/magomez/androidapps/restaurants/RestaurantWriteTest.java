package com.magomez.androidapps.restaurants;

import com.magomez.androidapps.restaurants.dao.RestaurantDao;
import com.magomez.androidapps.restaurants.dto.Restaurant;
import com.magomez.androidapps.restaurants.dto.RestaurantDTO;
import com.magomez.androidapps.restaurants.dto.SaveRestaurantRequest;
import com.magomez.androidapps.restaurants.dto.VisitRequest;
import com.magomez.androidapps.restaurants.service.RestaurantService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Writing JC Gourmet restaurants: validation first, values always as bind parameters. */
class RestaurantWriteTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final Clock CLOCK = Clock.fixed(ZonedDateTime.of(TODAY.atTime(12, 0), MADRID).toInstant(), MADRID);
    private static final Restaurant SAVED = new Restaurant(42, "Aponiente", "El Puerto", null, 4,
            null, null, null, null, TODAY, new BigDecimal("8.5"), "El arroz");

    private RecordingJdbcTemplate jdbc;
    private RestaurantService service;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
        service = new RestaurantService(new RestaurantDao(jdbc), CLOCK);
    }

    @Test
    void createTrimsBlanksToNullAndBindsEveryValue() {
        jdbc.nextResult = 42;

        RestaurantDTO created = service.create(new SaveRestaurantRequest("  Aponiente ", " El Puerto", " ", 4,
                "https://aponiente.com", "", "https://drive.google.com/drive/folders/abc", "Menú del mar"));

        assertThat(created.id()).isEqualTo(42);
        assertThat(created.name()).isEqualTo("Aponiente");
        assertThat(created.visit()).isNull();
        assertThat(jdbc.normalizedSql()).startsWith("INSERT INTO gourmet_restaurants").endsWith("RETURNING id");
        assertThat(jdbc.lastArgs).containsExactly("Aponiente", "El Puerto", null, 4, "https://aponiente.com", null,
                "https://drive.google.com/drive/folders/abc", "Menú del mar");
    }

    @Test
    void nameAndCityAreRequired() {
        assertBadRequest(() -> service.create(request(" ", "Madrid")));
        assertBadRequest(() -> service.create(request("DiverXO", null)));
        assertThat(jdbc.history).isEmpty();
    }

    @Test
    void priceLevelGoesFromOneToFour() {
        assertBadRequest(() -> service.create(new SaveRestaurantRequest("A", "B", null, 5, null, null, null, null)));
        assertBadRequest(() -> service.create(new SaveRestaurantRequest("A", "B", null, 0, null, null, null, null)));
    }

    @Test
    void linksMustBeWebAddresses() {
        assertBadRequest(() -> service.create(new SaveRestaurantRequest("A", "B", null, null, "aponiente", null, null, null)));
        assertBadRequest(() -> service.create(new SaveRestaurantRequest("A", "B", null, null, null, null, "javascript:alert(1)", null)));
    }

    @Test
    void tooLongNameIsRejected() {
        assertBadRequest(() -> service.create(request("x".repeat(121), "Madrid")));
    }

    @Test
    void updateOfAnUnknownRestaurantIsNotFound() {
        jdbc.nextUpdateCount = 0;

        assertStatus(() -> service.update(9, request("Saddle", "Madrid")), HttpStatus.NOT_FOUND);
    }

    @Test
    void updateBindsTheIdLast() {
        jdbc.nextResult = SAVED;

        service.update(42, request("Aponiente", "El Puerto"));

        assertThat(jdbc.history.get(0)).containsIgnoringCase("UPDATE gourmet_restaurants");
        assertThat(jdbc.history.get(1)).contains("WHERE id = ?");
    }

    @Test
    void visitWithAHalfPointRatingIsSaved() {
        jdbc.nextResult = SAVED;

        RestaurantDTO dto = service.saveVisit(42, new VisitRequest(TODAY, new BigDecimal("8.5"), " El arroz "));

        assertThat(dto.visit().rating()).isEqualByComparingTo("8.5");
        assertThat(jdbc.history.get(0).replaceAll("\\s+", " ")).contains("SET visited_on = ?, rating = ?, comment = ? WHERE id = ?");
    }

    @Test
    void ratingMustBeBetweenOneAndTenInHalfPoints() {
        assertBadRequest(() -> service.saveVisit(42, new VisitRequest(TODAY, new BigDecimal("8.3"), null)));
        assertBadRequest(() -> service.saveVisit(42, new VisitRequest(TODAY, new BigDecimal("0.5"), null)));
        assertBadRequest(() -> service.saveVisit(42, new VisitRequest(TODAY, new BigDecimal("10.5"), null)));
        assertBadRequest(() -> service.saveVisit(42, new VisitRequest(TODAY, null, null)));
        assertThat(jdbc.history).isEmpty();
    }

    @Test
    void visitNeedsADateThatIsNotInTheFuture() {
        assertBadRequest(() -> service.saveVisit(42, new VisitRequest(null, BigDecimal.TEN, null)));
        assertBadRequest(() -> service.saveVisit(42, new VisitRequest(TODAY.plusDays(1), BigDecimal.TEN, null)));
    }

    @Test
    void visitOfAnUnknownRestaurantIsNotFound() {
        jdbc.nextUpdateCount = 0;

        assertStatus(() -> service.saveVisit(9, new VisitRequest(TODAY, BigDecimal.ONE, null)), HttpStatus.NOT_FOUND);
    }

    @Test
    void clearingTheVisitEmptiesDateRatingAndComment() {
        jdbc.nextResult = SAVED;

        service.clearVisit(42);

        assertThat(jdbc.history.get(0).replaceAll("\\s+", " "))
                .contains("SET visited_on = NULL, rating = NULL, comment = NULL WHERE id = ?");
    }

    @Test
    void deleteBindsTheIdAndFailsWhenItDoesNotExist() {
        service.delete(42);
        assertThat(jdbc.normalizedSql()).isEqualTo("DELETE FROM gourmet_restaurants WHERE id = ?");
        assertThat(jdbc.lastArgs).containsExactly(42);

        jdbc.nextUpdateCount = 0;
        assertStatus(() -> service.delete(9), HttpStatus.NOT_FOUND);
    }

    private static SaveRestaurantRequest request(String name, String city) {
        return new SaveRestaurantRequest(name, city, null, null, null, null, null, null);
    }

    private static void assertBadRequest(ThrowingCallable call) {
        assertStatus(call, HttpStatus.BAD_REQUEST);
    }

    private static void assertStatus(ThrowingCallable call, HttpStatus status) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }
}
