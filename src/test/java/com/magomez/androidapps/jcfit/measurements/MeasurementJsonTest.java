package com.magomez.androidapps.jcfit.measurements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The JSON shapes the app parses: camelCase, ISO dates, numbers (never strings) and explicit nulls. */
class MeasurementJsonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Measurement MEASUREMENT = new Measurement(9, LocalDate.of(2026, 10, 13),
            new BigDecimal("78.4"), new BigDecimal("17.8"), new BigDecimal("101"), null, null, null, null,
            List.of(new MeasurementPhoto(3, MeasurementPose.FRONT)));

    @Test
    void measurementUsesTheContractNames() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(MEASUREMENT));

        assertThat(json.get("id").asInt()).isEqualTo(9);
        assertThat(json.get("measuredOn").asText()).isEqualTo("2026-10-13");
        assertThat(json.get("weightKg").isNumber()).isTrue();
        assertThat(json.get("weightKg").toString()).isEqualTo("78.4");
        assertThat(json.get("bodyFatPct").decimalValue()).isEqualByComparingTo("17.8");
        assertThat(json.get("chestCm").toString()).isEqualTo("101");
        for (String field : new String[]{"waistCm", "hipCm", "legCm", "notes"}) {
            assertThat(json.has(field)).as(field).isTrue();
            assertThat(json.get(field).isNull()).as(field).isTrue();
        }
        assertThat(json.get("photos").toString()).isEqualTo("[{\"id\":3,\"pose\":\"FRONT\"}]");
        assertThat(json.size()).isEqualTo(10);
    }

    @Test
    void emptyHistoryHasExplicitNulls() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(new MeasurementHistory(null, null, List.of())));

        assertThat(json.get("first").isNull()).isTrue();
        assertThat(json.get("latest").isNull()).isTrue();
        assertThat(json.get("items").toString()).isEqualTo("[]");
    }

    @Test
    void historyNestsFullMeasurements() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(
                new MeasurementHistory(MEASUREMENT, MEASUREMENT, List.of(MEASUREMENT))));

        assertThat(json.get("first").get("measuredOn").asText()).isEqualTo("2026-10-13");
        assertThat(json.get("latest").get("photos").get(0).get("pose").asText()).isEqualTo("FRONT");
        assertThat(json.get("items").get(0).get("weightKg").toString()).isEqualTo("78.4");
    }

    @Test
    void requestReadsTheContractBody() throws Exception {
        MeasurementRequest request = MAPPER.readValue("""
                {"measuredOn": "2026-10-13", "weightKg": 78.45, "bodyFatPct": null, "chestCm": 101,
                 "waistCm": 84.5, "hipCm": null, "legCm": null, "notes": "En ayunas"}
                """, MeasurementRequest.class);

        assertThat(request.measuredOn()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(request.weightKg()).isEqualByComparingTo("78.45");
        assertThat(request.chestCm()).isEqualByComparingTo("101");
        assertThat(request.waistCm()).isEqualByComparingTo("84.5");
        assertThat(request.bodyFatPct()).isNull();
        assertThat(request.notes()).isEqualTo("En ayunas");
    }
}
