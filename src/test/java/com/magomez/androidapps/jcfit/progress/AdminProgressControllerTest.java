package com.magomez.androidapps.jcfit.progress;

import com.magomez.androidapps.jcfit.auth.JcfitRecordingJdbcTemplate;
import com.magomez.androidapps.jcfit.config.JcfitApiExceptionHandler;
import com.magomez.androidapps.jcfit.config.JcfitConfig;
import com.magomez.androidapps.jcfit.measurements.MeasurementDao;
import com.magomez.androidapps.jcfit.measurements.MeasurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cris's progress, read-only for the backoffice: measurements and recent sessions. */
class AdminProgressControllerTest {

    private static final String URL = "/jcfit-api/v1/admin/progress";
    private static final String SESSIONS = "FROM recent r";

    private JcfitRecordingJdbcTemplate jdbc;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        MeasurementService measurements = new MeasurementService(new MeasurementDao(jdbc), Clock.system(JcfitConfig.SPAIN));
        AdminProgressService sessions = new AdminProgressService(new AdminProgressDao(jdbc));
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProgressController(measurements, sessions))
                .setControllerAdvice(new JcfitApiExceptionHandler())
                .build();
    }

    @Test
    void measurementsAreTheSameAsTheApps() throws Exception {
        jdbc.rows("FROM jcfit_measurements ORDER BY", measurement(9, "2026-10-13", "78.40"),
                measurement(2, "2026-09-29", "80.00"));
        jdbc.rows("FROM jcfit_photos", row("id", 3, "measurement_id", 9, "pose", "FRONT"));

        mockMvc.perform(get(URL + "/measurements").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.first.id").value(2))
                .andExpect(jsonPath("$.latest.id").value(9))
                .andExpect(jsonPath("$.latest.measuredOn").value("2026-10-13"))
                .andExpect(jsonPath("$.latest.weightKg").value(78.4))
                .andExpect(jsonPath("$.latest.photos[0].pose").value("FRONT"))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void sessionsComeNewestFirstWithCompletedSetsGroupedByExercise() throws Exception {
        jdbc.rows(SESSIONS,
                set(44, "2026-10-15", "Pierna", true, 4, "Peso muerto rumano", 2, "62.50", 9),
                set(44, "2026-10-15", "Pierna", true, 4, "Peso muerto rumano", 1, "62.50", 10),
                set(44, "2026-10-15", "Pierna", true, 1, "Sentadilla", 1, null, 12),
                set(41, "2026-10-12", "Empuje", false, null, null, null, null, null));

        mockMvc.perform(get(URL + "/sessions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {"sessionId": 44, "date": "2026-10-15", "dayName": "Pierna", "finished": true,
                           "exercises": [
                             {"exerciseId": 4, "name": "Peso muerto rumano",
                              "sets": [{"setNumber": 1, "weightKg": 62.5, "reps": 10},
                                       {"setNumber": 2, "weightKg": 62.5, "reps": 9}]},
                             {"exerciseId": 1, "name": "Sentadilla",
                              "sets": [{"setNumber": 1, "weightKg": null, "reps": 12}]}
                           ]},
                          {"sessionId": 41, "date": "2026-10-12", "dayName": "Empuje", "finished": false,
                           "exercises": []}
                        ]
                        """, true));

        assertThat(jdbc.calls).hasSize(1);
        assertThat(jdbc.last().args()).containsExactly(20);
        assertThat(jdbc.last().sql())
                .contains("ORDER BY session_date DESC, id DESC LIMIT ?")
                .contains("LEFT JOIN jcfit_set_logs l ON l.session_id = r.id AND l.completed")
                .contains("LEFT JOIN jcfit_exercises e ON e.id = l.exercise_id")
                .endsWith("ORDER BY r.session_date DESC, r.id DESC, l.id");
    }

    @Test
    void sessionsWithoutAnyAreAnEmptyList() throws Exception {
        mockMvc.perform(get(URL + "/sessions").param("limit", "5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));

        assertThat(jdbc.last().args()).containsExactly(5);
    }

    @Test
    void limitIs1To100() throws Exception {
        for (String limit : new String[]{"1", "100"}) {
            mockMvc.perform(get(URL + "/sessions").param("limit", limit).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
        for (String limit : new String[]{"0", "101", "x"}) {
            mockMvc.perform(get(URL + "/sessions").param("limit", limit).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("bad_request"));
        }
    }

    private static Map<String, Object> measurement(int id, String measuredOn, String weightKg) {
        return row("id", id, "measured_on", LocalDate.parse(measuredOn), "weight_kg", new BigDecimal(weightKg),
                "body_fat_pct", null, "chest_cm", null, "waist_cm", null, "hip_cm", null, "leg_cm", null,
                "notes", null);
    }

    private static Map<String, Object> set(int sessionId, String date, String dayName, boolean finished,
                                           Integer exerciseId, String name, Integer setNumber, String weightKg,
                                           Integer reps) {
        return row("session_id", sessionId, "session_date", LocalDate.parse(date), "day_name", dayName,
                "finished", finished, "exercise_id", exerciseId, "exercise_name", name, "set_number", setNumber,
                "weight_kg", weightKg == null ? null : new BigDecimal(weightKg), "reps", reps);
    }
}
