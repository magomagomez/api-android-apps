package com.magomez.androidapps.movierec;

import com.magomez.androidapps.movierec.controller.ScheduleController;
import com.magomez.androidapps.movierec.schedule.ScheduleDay;
import com.magomez.androidapps.movierec.schedule.ScheduleResult;
import com.magomez.androidapps.movierec.schedule.ScheduleService;
import com.magomez.androidapps.movierec.schedule.ScheduledSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint contract test for {@code POST /api/schedule}. Uses a hand-written
 * {@link ScheduleService} stub (concrete class), a standalone MockMvc, no external API.
 */
class ScheduleControllerTest {

    private List<String> lastTitles;
    private Function<List<String>, ScheduleResult> handler = t -> new ScheduleResult(t, List.of(), List.of());
    private boolean ready = true;
    private boolean failWithIOException = false;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ScheduleService stub = new ScheduleService(null) {
            @Override
            public boolean isReady() {
                return ready;
            }

            @Override
            public ScheduleResult buildSchedule(List<String> titles) throws IOException {
                if (failWithIOException) {
                    throw new IOException("Sitges is down");
                }
                lastTitles = titles;
                return handler.apply(titles);
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new ScheduleController(stub)).build();
    }

    @Test
    void returnsTheCalendarAsJson() throws Exception {
        handler = titles -> new ScheduleResult(titles, List.of("Ghost Film"), List.of(
                new ScheduleDay(LocalDate.of(2026, 10, 12), List.of(
                        new ScheduledSession("Buddy", LocalTime.of(20, 0), LocalTime.of(21, 41), "Auditori",
                                true, List.of("Buddy"))))));

        mockMvc.perform(post("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titles\":[\"Buddy\",\"Ghost Film\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notScheduled[0]").value("Ghost Film"))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-12"))
                .andExpect(jsonPath("$.days[0].weekday").value("Lunes"))
                .andExpect(jsonPath("$.days[0].holiday").value(true))
                .andExpect(jsonPath("$.days[0].sessions[0].title").value("Buddy"))
                .andExpect(jsonPath("$.days[0].sessions[0].startTime").value("20:00"))
                .andExpect(jsonPath("$.days[0].sessions[0].convenient").value(true));
    }

    @Test
    void anEmptyTitlesListIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titles\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whileTheProgrammeIsStillWarmingUpItFailsFastInsteadOfBlocking() throws Exception {
        ready = false;

        mockMvc.perform(post("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titles\":[\"Buddy\"]}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void aScheduleFetchFailureBecomesABadGateway() throws Exception {
        failWithIOException = true;

        mockMvc.perform(post("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titles\":[\"Buddy\"]}"))
                .andExpect(status().isBadGateway());
    }
}
