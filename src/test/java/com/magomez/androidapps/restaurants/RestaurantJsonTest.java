package com.magomez.androidapps.restaurants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.magomez.androidapps.restaurants.dto.VisitDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** The app reads the visit date as an ISO string, whatever the mapper's date defaults are. */
class RestaurantJsonTest {

    @Test
    void visitDateIsAnIsoString() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        String json = mapper.writeValueAsString(new VisitDTO(LocalDate.of(2026, 3, 14), new BigDecimal("8.5"), null));

        assertThat(json).contains("\"date\":\"2026-03-14\"").contains("\"rating\":8.5");
    }
}
