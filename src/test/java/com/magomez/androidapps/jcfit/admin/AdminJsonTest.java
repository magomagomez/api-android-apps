package com.magomez.androidapps.jcfit.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The JSON shapes the backoffice parses: camelCase, yyyy-MM-dd, explicit nulls. */
class AdminJsonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void exerciseUsesTheContractNamesWithExplicitNulls() throws Exception {
        AdminExercise exercise = new AdminExercise(4, "Peso muerto rumano", null, null, null, "Femoral", false);

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(exercise));

        assertThat(json.fieldNames()).toIterable().containsExactly("id", "name", "youtubeUrl", "youtubeVideoId",
                "instructions", "muscleGroup", "archived");
        assertThat(json.get("youtubeUrl").isNull()).isTrue();
        assertThat(json.get("archived").asBoolean()).isFalse();
    }

    @Test
    void programNestsDaysAndExercises() throws Exception {
        AdminProgram program = new AdminProgram(1, "Recomposición", LocalDate.of(2026, 9, 1), List.of(
                new AdminProgramDay(3, 1, "Empuje", null, List.of(
                        new AdminProgramExercise(11, 4, "Press banca", false, 1, 4, 6, 8, null)))));

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(program));

        assertThat(json.get("startDate").asText()).isEqualTo("2026-09-01");
        JsonNode day = json.get("days").get(0);
        assertThat(day.fieldNames()).toIterable().containsExactly("id", "dayOfWeek", "name", "muscles", "exercises");
        assertThat(day.get("muscles").isNull()).isTrue();
        JsonNode line = day.get("exercises").get(0);
        assertThat(line.fieldNames()).toIterable().containsExactly("id", "exerciseId", "exerciseName", "archived",
                "position", "sets", "repsMin", "repsMax", "restSeconds");
        assertThat(line.get("restSeconds").isNull()).isTrue();
    }

    @Test
    void requestsReadTheContractBodies() throws Exception {
        AdminProgramRequest program = MAPPER.readValue("{\"name\":\"Recomposición\",\"startDate\":\"2026-09-01\"}",
                AdminProgramRequest.class);
        AdminProgramDayRequest day = MAPPER.readValue("{\"name\":\"Empuje\",\"muscles\":null,\"exercises\":"
                + "[{\"exerciseId\":4,\"sets\":4,\"repsMin\":6,\"repsMax\":8,\"restSeconds\":null}]}",
                AdminProgramDayRequest.class);
        AdminExerciseRequest exercise = MAPPER.readValue("{\"name\":\"Remo\",\"archived\":true}",
                AdminExerciseRequest.class);

        assertThat(program.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(day.exercises().get(0)).isEqualTo(new AdminProgramExerciseRequest(4, 4, 6, 8, null));
        assertThat(exercise.archived()).isTrue();
        assertThat(exercise.youtubeUrl()).isNull();
    }
}
