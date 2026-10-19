package com.magomez.androidapps.jcfit.notifications;

import com.magomez.androidapps.jcfit.auth.JcfitCurrentUser;
import com.magomez.androidapps.jcfit.auth.JcfitRecordingJdbcTemplate;
import com.magomez.androidapps.jcfit.config.JcfitApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PUT and DELETE /devices over the real service and DAO: the FCM tokens of the app account. */
class JcfitDeviceControllerTest {

    private static final String URL = "/jcfit-api/v1/devices";
    private static final int CRIS = 2;
    private static final JcfitCurrentUser USER = new JcfitCurrentUser(CRIS, "Cris", "h".repeat(64));
    private static final String UPSERT = """
            INSERT INTO jcfit_devices (token, user_id) VALUES (?, ?) \
            ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, updated_at = now()""";

    private JcfitRecordingJdbcTemplate jdbc;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        JcfitDeviceService service = new JcfitDeviceService(new JcfitDeviceDao(jdbc));
        mockMvc = MockMvcBuilders.standaloneSetup(new JcfitDeviceController(service))
                .setControllerAdvice(new JcfitApiExceptionHandler())
                .build();
    }

    @Test
    void putStoresOrRefreshesTheTokenForTheUserOfTheRequest() throws Exception {
        mockMvc.perform(withBody(put(URL), "{\"token\": \"fcm-token-1\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(jdbc.last().sql()).isEqualTo(UPSERT);
        assertThat(jdbc.last().args()).containsExactly("fcm-token-1", CRIS);
    }

    @Test
    void repeatingThePutIsTheSameUpsert() throws Exception {
        mockMvc.perform(withBody(put(URL), "{\"token\": \"fcm-token-1\"}")).andExpect(status().isNoContent());
        mockMvc.perform(withBody(put(URL), "{\"token\": \"fcm-token-1\"}")).andExpect(status().isNoContent());

        assertThat(jdbc.callsWith("INSERT INTO jcfit_devices")).hasSize(2)
                .allSatisfy(call -> assertThat(call.args()).containsExactly("fcm-token-1", CRIS));
    }

    @Test
    void theTokenIsTrimmed() throws Exception {
        mockMvc.perform(withBody(put(URL), "{\"token\": \"  fcm-token-1 \"}")).andExpect(status().isNoContent());

        assertThat(jdbc.last().args()).containsExactly("fcm-token-1", CRIS);
    }

    @Test
    void aTokenOf4096CharactersIsAccepted() throws Exception {
        String longest = "t".repeat(4096);

        mockMvc.perform(withBody(put(URL), "{\"token\": \"" + longest + "\"}")).andExpect(status().isNoContent());

        assertThat(jdbc.last().args()).containsExactly(longest, CRIS);
    }

    @Test
    void emptyBlankMissingOrTooLongTokenIs400() throws Exception {
        for (String body : new String[]{"{\"token\": \"\"}", "{\"token\": \"   \"}", "{\"token\": null}", "{}",
                "{\"token\": \"" + "t".repeat(4097) + "\"}"}) {
            for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[]{put(URL), delete(URL)}) {
                mockMvc.perform(withBody(request, body))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("bad_request"))
                        .andExpect(jsonPath("$.message").value("El token tiene que tener entre 1 y 4096 caracteres"));
            }
        }
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void missingBodyIs400() throws Exception {
        mockMvc.perform(put(URL).requestAttr(JcfitCurrentUser.ATTRIBUTE, USER).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void deleteRemovesTheToken() throws Exception {
        mockMvc.perform(withBody(delete(URL), "{\"token\": \"fcm-token-1\"}")).andExpect(status().isNoContent());

        assertThat(jdbc.last().sql()).isEqualTo("DELETE FROM jcfit_devices WHERE token = ?");
        assertThat(jdbc.last().args()).containsExactly("fcm-token-1");
    }

    @Test
    void deletingAnUnknownTokenIsStill204() throws Exception {
        jdbc.updateCount("DELETE FROM jcfit_devices", 0);

        mockMvc.perform(withBody(delete(URL), "{\"token\": \"no-existe\"}")).andExpect(status().isNoContent());
    }

    private static MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder request, String body) {
        return request.requestAttr(JcfitCurrentUser.ATTRIBUTE, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
