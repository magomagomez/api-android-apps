package com.magomez.androidapps.restaurants;

import com.magomez.androidapps.restaurants.converter.RestaurantConverter;
import com.magomez.androidapps.restaurants.dao.RestaurantDao;
import com.magomez.androidapps.restaurants.dto.Restaurant;
import com.magomez.androidapps.restaurants.dto.RestaurantDTO;
import com.magomez.androidapps.restaurants.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reading JC Gourmet restaurants: pending ones by name, tried ones by rating. */
class RestaurantReadTest {

    private static final Restaurant TRIED = new Restaurant(7, "Ricard Camarena", "Valencia", "Creativa", 4,
            "https://ricardcamarena.com", "C/ Doctor Sumsi 4", "https://drive.google.com/drive/folders/abc",
            "Las verduras", LocalDate.of(2026, 3, 14), new BigDecimal("9.0"), "Volver en temporada");
    private static final Restaurant PENDING = new Restaurant(8, "Aponiente", "El Puerto de Santa María", null, 4,
            null, null, null, "Menú del mar", null, null, null);

    private RecordingJdbcTemplate jdbc;
    private RestaurantService service;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
        service = new RestaurantService(new RestaurantDao(jdbc), java.time.Clock.systemUTC());
    }

    @Test
    void withoutFilterListsEveryRestaurantByName() {
        service.list(null);

        assertThat(jdbc.normalizedSql()).doesNotContain("WHERE").endsWith("ORDER BY name");
    }

    @Test
    void pendingAreTheOnesWithoutAVisitByName() {
        service.list(0);

        assertThat(jdbc.normalizedSql()).endsWith("WHERE visited_on IS NULL ORDER BY name");
    }

    @Test
    void triedAreTheOnesWithAVisitBestRatedFirst() {
        service.list(1);

        assertThat(jdbc.normalizedSql()).endsWith("WHERE visited_on IS NOT NULL ORDER BY rating DESC, visited_on DESC, name");
    }

    @Test
    void anyOtherVisitedValueIsABadRequest() {
        assertThatThrownBy(() -> service.list(2))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(jdbc.history).isEmpty();
    }

    @Test
    void listReturnsTheRowsAsDtos() {
        jdbc.nextList = List.of(TRIED, PENDING);

        assertThat(service.list(null)).extracting(RestaurantDTO::name).containsExactly("Ricard Camarena", "Aponiente");
    }

    @Test
    void getByIdBindsTheIdAsAParameter() {
        jdbc.nextResult = TRIED;

        RestaurantDTO dto = service.get(7);

        assertThat(dto.id()).isEqualTo(7);
        assertThat(jdbc.normalizedSql()).endsWith("WHERE id = ?");
        assertThat(jdbc.lastArgs).containsExactly(7);
    }

    @Test
    void unknownIdIsNotFound() {
        jdbc.nextFailure = new EmptyResultDataAccessException(1);

        assertThatThrownBy(() -> service.get(404))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void aTriedRestaurantCarriesItsVisit() {
        RestaurantDTO dto = RestaurantConverter.toDto(TRIED);

        assertThat(dto.visit()).isNotNull();
        assertThat(dto.visit().date()).isEqualTo(LocalDate.of(2026, 3, 14));
        assertThat(dto.visit().rating()).isEqualByComparingTo("9");
        assertThat(dto.visit().comment()).isEqualTo("Volver en temporada");
        assertThat(dto.driveFolderUrl()).isEqualTo("https://drive.google.com/drive/folders/abc");
    }

    @Test
    void aPendingRestaurantHasNoVisit() {
        assertThat(RestaurantConverter.toDto(PENDING).visit()).isNull();
    }
}
