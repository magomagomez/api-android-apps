package com.magomez.androidapps.jcfit.notifications;

import com.magomez.androidapps.jcfit.auth.JcfitRecordingJdbcTemplate;
import com.magomez.androidapps.jcfit.config.JcfitApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static com.magomez.androidapps.jcfit.notifications.AdminNotificationServiceTest.notificationRow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routes, status codes and JSON of /admin/notifications over the real service and DAOs. */
class AdminNotificationControllerTest {

    private static final String URL = "/jcfit-api/v1/admin/notifications";
    private static final String BODY = "{\"title\": \"¡A por la semana!\", \"body\": \"Ánimo\"}";

    private final JcfitRecordingJdbcTemplate jdbc = new JcfitRecordingJdbcTemplate()
            .scalar("nextval(", 3L);

    @Test
    void postAnswers201WithTheNotification() throws Exception {
        jdbc.rows("SELECT token FROM jcfit_devices", row("token", "a"))
                .rows("INSERT INTO jcfit_notifications", notificationRow(3, 1, 0, "¡A por la semana!", "Ánimo"));

        mockMvc(new FakePushSender()).perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"id": 3, "title": "¡A por la semana!", "body": "Ánimo",
                         "sentAt": "2026-10-19T08:00:00Z", "delivered": 1, "failed": 0}
                        """, true));
    }

    @Test
    void unconfiguredIs503() throws Exception {
        mockMvc(new FakePushSender().unconfigured()).perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("""
                        {"error": "notifications_unavailable",
                         "message": "Las notificaciones todavía no están configuradas"}
                        """, true));
    }

    @Test
    void generalFcmFailureIs502() throws Exception {
        jdbc.rows("SELECT token FROM jcfit_devices", row("token", "a"));

        mockMvc(new FakePushSender().answer("a", JcfitPushResult.FAILED))
                .perform(post(URL).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadGateway())
                .andExpect(content().json("""
                        {"error": "fcm_error", "message": "No se ha podido enviar el aviso; inténtalo más tarde"}
                        """, true));
    }

    @Test
    void invalidFieldIs400WithTheField() throws Exception {
        mockMvc(new FakePushSender()).perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON).content("{\"title\": \"Hola\", \"body\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.message").value("El texto es obligatorio y tiene como máximo 300 caracteres"));
    }

    @Test
    void getListsWithTheLimit() throws Exception {
        jdbc.rows("FROM jcfit_notifications ORDER BY", notificationRow(5, 1, 0), notificationRow(4, 0, 1));

        mockMvc(new FakePushSender()).perform(get(URL).param("limit", "2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].sentAt").value("2026-10-19T08:00:00Z"))
                .andExpect(jsonPath("$[1].failed").value(1));
    }

    @Test
    void badLimitIs400() throws Exception {
        MockMvc mockMvc = mockMvc(new FakePushSender());
        for (String limit : new String[]{"0", "101", "x"}) {
            mockMvc.perform(get(URL).param("limit", limit).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("bad_request"));
        }
    }

    private MockMvc mockMvc(JcfitPushSender sender) {
        AdminNotificationService service = new AdminNotificationService(new JcfitDeviceDao(jdbc),
                new JcfitNotificationDao(jdbc), sender, 2);
        return MockMvcBuilders.standaloneSetup(new AdminNotificationController(service))
                .setControllerAdvice(new JcfitApiExceptionHandler())
                .build();
    }
}
