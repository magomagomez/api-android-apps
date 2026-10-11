package com.magomez.androidapps.escapersthings.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.magomez.androidapps.escapersthings.v2.auth.UserDTO;
import com.magomez.androidapps.escapersthings.v2.rooms.Averages;
import com.magomez.androidapps.escapersthings.v2.rooms.Grade;
import com.magomez.androidapps.escapersthings.v2.rooms.PhotoRef;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomDetail;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomSummary;
import com.magomez.androidapps.escapersthings.v2.rooms.UserGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The JSON shapes the app parses: camelCase, ISO dates, explicit nulls, detail = summary + grades. */
class V2JsonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Grade GRADE = new Grade(new BigDecimal("8"), new BigDecimal("7.25"), BigDecimal.ZERO, null,
            new BigDecimal("8.5"));
    private static final RoomSummary SUMMARY = new RoomSummary(7, "Profana", "Terror", null, List.of(1, 2), true,
            LocalDate.of(2026, 5, 3), new Averages(null, null, null, null, new BigDecimal("9.88")), 1, GRADE,
            new PhotoRef("ab12"), null);

    @Test
    void summaryUsesTheContractNames() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(SUMMARY));

        assertThat(json.get("playedOn").asText()).isEqualTo("2026-05-03");
        assertThat(json.get("playerIds").toString()).isEqualTo("[1,2]");
        assertThat(json.get("city").isNull()).isTrue();
        assertThat(json.get("groupPhoto").isNull()).isTrue();
        assertThat(json.get("poster").get("etag").asText()).isEqualTo("ab12");
        assertThat(json.get("averages").get("global").decimalValue()).isEqualByComparingTo("9.88");
        assertThat(json.get("myGrade").get("gameMaster").isNull()).isTrue();
        assertThat(json.get("myGrade").get("terror").decimalValue()).isEqualByComparingTo("0");
        assertThat(json.get("myGrade").has("hasAnyScore")).isFalse();
        assertThat(json.has("gradeCount")).isTrue();
    }

    @Test
    void detailIsTheSummaryPlusGrades() throws Exception {
        RoomDetail detail = new RoomDetail(SUMMARY, List.of(new UserGrade(new UserDTO(1, "Magomez"), GRADE)));

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(detail));

        assertThat(json.has("summary")).isFalse();
        assertThat(json.get("name").asText()).isEqualTo("Profana");
        assertThat(json.get("grades").get(0).get("user").get("name").asText()).isEqualTo("Magomez");
        assertThat(json.get("grades").get(0).get("grade").get("immersion").decimalValue()).isEqualByComparingTo("7.25");
    }
}
