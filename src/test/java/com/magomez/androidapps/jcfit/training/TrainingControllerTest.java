package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.JcfitApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.magomez.androidapps.jcfit.training.TrainingFixtures.ACTIVE_DAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.ACTIVE_PROGRAM;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.EXERCISE_NAME;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.FINISH;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.INSERT_SESSION;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.IS_IN_DAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.SESSION_BY_ID;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.SESSION_OF_DAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.WEDNESDAY;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.WEDNESDAY_NOON;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.activeDay;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.row;
import static com.magomez.androidapps.jcfit.training.TrainingFixtures.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routes, status codes and error bodies of the training endpoints over the fake database. */
class TrainingControllerTest {

    private static final String BASE = "/jcfit-api/v1";
    private static final String START_BODY = "{\"programDayId\": 5, \"date\": \"2026-10-14\"}";

    private RecordingJdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate()
                .rows(ACTIVE_DAY, activeDay(5, 3, "Pierna"))
                .rows(INSERT_SESSION, session(44, 5, WEDNESDAY, null))
                .rows(SESSION_OF_DAY, session(44, 5, WEDNESDAY, null))
                .rows(SESSION_BY_ID, session(44, 5, WEDNESDAY, null))
                .rows(FINISH, session(44, 5, WEDNESDAY, "2026-10-14T18:30:00Z"))
                .rows(EXERCISE_NAME, row("name", "Sentadilla"))
                .scalar(IS_IN_DAY, 1);
        TrainingProgramDao programDao = new TrainingProgramDao(jdbc);
        WorkoutSessionDao sessionDao = new WorkoutSessionDao(jdbc);
        SetLogDao setLogDao = new SetLogDao(jdbc);
        TrainingController training = new TrainingController(
                new WeekService(programDao, sessionDao, setLogDao, WEDNESDAY_NOON),
                new WorkoutDayService(programDao, sessionDao, setLogDao),
                new ExerciseHistoryService(programDao, setLogDao));
        WorkoutSessionController sessions = new WorkoutSessionController(
                new WorkoutSessionService(programDao, sessionDao, setLogDao, WEDNESDAY_NOON));
        mvc = MockMvcBuilders.standaloneSetup(training, sessions)
                .setControllerAdvice(new JcfitApiExceptionHandler())
                .build();
    }

    @Test
    void startingIsCreatedTheFirstTimeAndOkAfterwards() throws Exception {
        mvc.perform(post(BASE + "/sessions").contentType(MediaType.APPLICATION_JSON).content(START_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(44))
                .andExpect(jsonPath("$.startedAt").value("2026-10-14T17:40:00Z"))
                .andExpect(jsonPath("$.finishedAt").isEmpty());

        jdbc.rows(INSERT_SESSION);
        mvc.perform(post(BASE + "/sessions").contentType(MediaType.APPLICATION_JSON).content(START_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(44));
    }

    @Test
    void aFutureDayIsAnErrorBody() throws Exception {
        mvc.perform(post(BASE + "/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programDayId\": 5, \"date\": \"2026-10-21\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("future_date"))
                .andExpect(jsonPath("$.message").value("No puedes empezar un entreno de un día futuro"));
    }

    @Test
    void savingASetAnswersWithItsNumbers() throws Exception {
        mvc.perform(put(BASE + "/sessions/44/sets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\": 4, \"setNumber\": 2, \"weightKg\": 62.5, \"reps\": 10, \"completed\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setNumber").value(2))
                .andExpect(jsonPath("$.weightKg").value(62.5))
                .andExpect(jsonPath("$.reps").value(10))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void deletingASetIsNoContent() throws Exception {
        mvc.perform(delete(BASE + "/sessions/44/sets").param("exerciseId", "4").param("setNumber", "4"))
                .andExpect(status().isNoContent());
        mvc.perform(delete(BASE + "/sessions/44/sets").param("exerciseId", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void finishingAnswersTheSession() throws Exception {
        mvc.perform(post(BASE + "/sessions/44/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finishedAt").value("2026-10-14T18:30:00Z"));
    }

    @Test
    void aWeekWithoutProgramIsNotFound() throws Exception {
        mvc.perform(get(BASE + "/weeks/2026-10-14"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("no_active_program"))
                .andExpect(jsonPath("$.message").value("Todavía no hay un programa activo"));
        assertThat(jdbc.callsWith(ACTIVE_PROGRAM)).hasSize(1);
    }

    @Test
    void badDatesAndMissingParametersAreBadRequests() throws Exception {
        mvc.perform(get(BASE + "/weeks/14-10-2026")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
        mvc.perform(get(BASE + "/days/5")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/days/5").param("date", "2026-10-15")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha no corresponde a ese día del programa"));
    }

    @Test
    void theDayIsServed() throws Exception {
        mvc.perform(get(BASE + "/days/5").param("date", "2026-10-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programDayId").value(5))
                .andExpect(jsonPath("$.sessionId").value(44))
                .andExpect(jsonPath("$.lastDate").isEmpty());
    }

    @Test
    void historyTakesTheLimit() throws Exception {
        mvc.perform(get(BASE + "/exercises/4/history").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sentadilla"))
                .andExpect(jsonPath("$.best").isEmpty());
        mvc.perform(get(BASE + "/exercises/4/history").param("limit", "500"))
                .andExpect(status().isBadRequest());
    }
}
