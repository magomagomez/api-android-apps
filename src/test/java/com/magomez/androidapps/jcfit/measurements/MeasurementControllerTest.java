package com.magomez.androidapps.jcfit.measurements;

import com.magomez.androidapps.jcfit.config.JcfitApiExceptionHandler;
import com.magomez.androidapps.jcfit.config.JcfitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;

import static com.magomez.androidapps.jcfit.measurements.MeasurementServiceTest.measurement;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routes, status codes and error bodies of /jcfit-api/v1/measurements over the real service and DAO. */
class MeasurementControllerTest {

    private static final String URL = "/jcfit-api/v1/measurements";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), JcfitConfig.SPAIN);
    private static final String BODY = """
            {"measuredOn": "2026-10-18", "weightKg": 78.4, "bodyFatPct": null, "chestCm": null,
             "waistCm": null, "hipCm": null, "legCm": null, "notes": null}
            """;

    private MeasurementFakeJdbcTemplate jdbc;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbc = new MeasurementFakeJdbcTemplate();
        MeasurementService service = new MeasurementService(new MeasurementDao(jdbc), CLOCK);
        mockMvc = MockMvcBuilders.standaloneSetup(new MeasurementController(service))
                .setControllerAdvice(new JcfitApiExceptionHandler())
                .build();
    }

    @Test
    void listReturnsFirstLatestAndItems() throws Exception {
        jdbc.rows("FROM jcfit_measurements ORDER BY", measurement(9, "2026-10-13", "78.40"),
                measurement(2, "2026-09-29", "80.00"));

        mockMvc.perform(get(URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.first.id").value(2))
                .andExpect(jsonPath("$.latest.id").value(9))
                .andExpect(jsonPath("$.latest.measuredOn").value("2026-10-13"))
                .andExpect(jsonPath("$.latest.weightKg").value(78.4))
                .andExpect(jsonPath("$.latest.notes").isEmpty())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void createAnswers201WithTheMeasurement() throws Exception {
        jdbc.scalar("INSERT INTO jcfit_measurements", 12)
                .rows("FROM jcfit_measurements WHERE id = ?", measurement(12, "2026-10-18", "78.40"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.photos").isArray());
    }

    @Test
    void duplicateDayAnswers409WithTheContractBody() throws Exception {
        jdbc.fail("INSERT INTO jcfit_measurements", new DuplicateKeyException("measured_on"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("measurement_exists"))
                .andExpect(jsonPath("$.message").value("Ya hay un registro ese día"));
    }

    @Test
    void invalidBodyAnswers400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content("{\"measuredOn\": \"2026-10-18\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.message").value("Rellena al menos una medida"));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(BODY.replace("2026-10-18", "2026-10-19")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("future_date"));
    }

    @Test
    void updateOfAMissingMeasurementAnswers404() throws Exception {
        jdbc.updateCount("UPDATE jcfit_measurements", 0);

        mockMvc.perform(put(URL + "/99").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void deleteAnswers204Or404() throws Exception {
        mockMvc.perform(delete(URL + "/9")).andExpect(status().isNoContent());

        jdbc.updateCount("DELETE FROM jcfit_measurements", 0);
        mockMvc.perform(delete(URL + "/99")).andExpect(status().isNotFound());
    }
}
