package com.magomez.androidapps.jcfit.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The JSON shapes the app parses: contract names, ISO dates and instants, numbers for kg, explicit nulls. */
class TrainingJsonTest {

    /** Built like the app's: @EnableWebMvc skips Boot's Jackson defaults, so the records format their dates. */
    private static final ObjectMapper MAPPER = Jackson2ObjectMapperBuilder.json().build();
    private static final LocalDate DATE = LocalDate.of(2026, 10, 15);

    @Test
    void weekUsesTheContractNames() throws Exception {
        WeekView week = new WeekView(6, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18), DATE, 5, 2, List.of(
                new WeekDay(LocalDate.of(2026, 10, 12), 1, DayStatus.DONE, 3, "Empuje", "Pecho · Hombro", 6, 6, 41),
                new WeekDay(LocalDate.of(2026, 10, 14), 3, DayStatus.REST, null, null, null, 0, 0, null)));

        JsonNode json = json(week);

        assertThat(fields(json)).containsExactly("weekNumber", "startDate", "endDate", "today", "planned", "completed",
                "days");
        assertThat(json.get("startDate").asText()).isEqualTo("2026-10-12");
        assertThat(json.get("today").asText()).isEqualTo("2026-10-15");
        JsonNode done = json.get("days").get(0);
        assertThat(fields(done)).containsExactly("date", "dayOfWeek", "status", "programDayId", "name", "muscles",
                "exerciseCount", "completedExercises", "sessionId");
        assertThat(done.get("status").asText()).isEqualTo("DONE");
        JsonNode rest = json.get("days").get(1);
        assertThat(rest.get("programDayId").isNull()).isTrue();
        assertThat(rest.get("sessionId").isNull()).isTrue();
        assertThat(rest.get("name").isNull()).isTrue();
    }

    @Test
    void dayUsesTheContractNamesWithNumbersForKg() throws Exception {
        DayExercise exercise = new DayExercise(11, 4, 3, "Peso muerto rumano", "https://youtu.be/dQw4w9WgXcQ",
                "dQw4w9WgXcQ", "Cadera atrás", 3, 8, 10, null,
                new PreviousSets(LocalDate.of(2026, 10, 8), List.of(new SetEntry(1, new BigDecimal("60.00"), 10))),
                List.of(new LoggedSet(1, new BigDecimal("62.50"), 10, true), new LoggedSet(2, null, null, false)));
        WorkoutDayView day = new WorkoutDayView(3, DATE, 4, 6, "Pierna", "Cuádriceps", 44,
                Instant.parse("2026-10-15T17:40:00Z"), null, LocalDate.of(2026, 10, 8), List.of(exercise));

        JsonNode json = json(day);

        assertThat(fields(json)).containsExactly("programDayId", "date", "dayOfWeek", "weekNumber", "name", "muscles",
                "sessionId", "startedAt", "finishedAt", "lastDate", "exercises");
        assertThat(json.get("startedAt").asText()).isEqualTo("2026-10-15T17:40:00Z");
        assertThat(json.get("finishedAt").isNull()).isTrue();
        assertThat(json.get("lastDate").asText()).isEqualTo("2026-10-08");
        JsonNode first = json.get("exercises").get(0);
        assertThat(fields(first)).containsExactly("programExerciseId", "exerciseId", "position", "name", "youtubeUrl",
                "youtubeVideoId", "instructions", "sets", "repsMin", "repsMax", "restSeconds", "previous", "logged");
        assertThat(first.get("restSeconds").isNull()).isTrue();
        assertThat(first.get("previous").get("date").asText()).isEqualTo("2026-10-08");
        assertThat(fields(first.get("previous").get("sets").get(0))).containsExactly("setNumber", "weightKg", "reps");
        assertThat(first.get("logged").get(0).get("weightKg").isNumber()).isTrue();
        assertThat(first.get("logged").get(0).get("weightKg").decimalValue()).isEqualByComparingTo("62.5");
        assertThat(fields(first.get("logged").get(1))).containsExactly("setNumber", "weightKg", "reps", "completed");
        assertThat(first.get("logged").get(1).get("weightKg").isNull()).isTrue();
    }

    @Test
    void aDayWithoutPreviousHasItAsNull() throws Exception {
        DayExercise exercise = new DayExercise(11, 4, 1, "Sentadilla", null, null, null, 3, 8, 10, 90, null, List.of());

        JsonNode json = json(exercise);

        assertThat(json.get("previous").isNull()).isTrue();
        assertThat(json.get("youtubeUrl").isNull()).isTrue();
        assertThat(json.get("logged").isEmpty()).isTrue();
    }

    @Test
    void sessionUsesTheContractNames() throws Exception {
        JsonNode json = json(new SessionReply(44, Instant.parse("2026-10-15T17:40:00Z"), null));

        assertThat(fields(json)).containsExactly("sessionId", "startedAt", "finishedAt");
        assertThat(json.get("startedAt").asText()).isEqualTo("2026-10-15T17:40:00Z");
        assertThat(json.get("finishedAt").isNull()).isTrue();
    }

    @Test
    void historyUsesTheContractNames() throws Exception {
        ExerciseHistory history = new ExerciseHistory(4, "Peso muerto rumano",
                new BestSet(new BigDecimal("62.50"), 10, DATE), 7, List.of(new HistorySession(44, DATE, "Pierna",
                new BigDecimal("62.50"), new BigDecimal("625.00"), 10,
                List.of(new SetEntry(1, new BigDecimal("62.50"), 10)))));

        JsonNode json = json(history);

        assertThat(fields(json)).containsExactly("exerciseId", "name", "best", "sessionCount", "sessions");
        assertThat(fields(json.get("best"))).containsExactly("weightKg", "reps", "date");
        assertThat(json.get("best").get("weightKg").isNumber()).isTrue();
        JsonNode session = json.get("sessions").get(0);
        assertThat(fields(session)).containsExactly("sessionId", "date", "dayName", "topWeightKg", "volumeKg",
                "totalReps", "sets");
        assertThat(session.get("volumeKg").decimalValue()).isEqualByComparingTo("625");
    }

    @Test
    void historyWithoutSetsHasNullBest() throws Exception {
        JsonNode json = json(new ExerciseHistory(4, "Sentadilla", null, 0, List.of()));

        assertThat(json.get("best").isNull()).isTrue();
    }

    private static JsonNode json(Object value) throws Exception {
        return MAPPER.readTree(MAPPER.writeValueAsString(value));
    }

    private static List<String> fields(JsonNode node) {
        List<String> names = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
