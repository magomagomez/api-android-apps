package com.magomez.androidapps.jcfit.auth;

import com.magomez.androidapps.jcfit.config.JcfitApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Both logins and logouts over the real service and DAOs: each site lets in only its account. */
class JcfitAuthControllerTest {

    private static final JcfitPasswordHasher HASHER = new JcfitPasswordHasher();
    private static final String PASSWORD = "sentadillas";
    private static final String STORED = HASHER.hash(PASSWORD);
    private static final String BY_NAME = "FROM users WHERE name = ?";
    private static final String APP_LOGIN = "/jcfit-api/v1/auth/login";
    private static final String ADMIN_LOGIN = "/jcfit-api/v1/admin/auth/login";
    private static final int CRIS = 2;
    private static final int MAGOMEZ = 1;

    private JcfitRecordingJdbcTemplate jdbc;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        JcfitAuthService service = new JcfitAuthService(new JcfitUserDao(jdbc), new JcfitTokenDao(jdbc), HASHER,
                CRIS, MAGOMEZ);
        mockMvc = MockMvcBuilders.standaloneSetup(new JcfitAuthController(service),
                        new JcfitAdminAuthController(service))
                .setControllerAdvice(new JcfitApiExceptionHandler())
                .build();
    }

    @Test
    void theAppAccountLogsInToTheApp() throws Exception {
        jdbc.rows(BY_NAME, row("id", CRIS, "name", "Cris", "password_hash", STORED));

        mockMvc.perform(login(APP_LOGIN, "Cris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void theBackofficeAccountLogsInToTheBackoffice() throws Exception {
        jdbc.rows(BY_NAME, row("id", MAGOMEZ, "name", "Magomez", "password_hash", STORED));

        mockMvc.perform(login(ADMIN_LOGIN, "Magomez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void eachAccountGetsTheSame401AtTheOtherSite() throws Exception {
        jdbc.rows(BY_NAME, row("id", MAGOMEZ, "name", "Magomez", "password_hash", STORED));
        mockMvc.perform(login(APP_LOGIN, "Magomez"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"))
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));

        jdbc.rows(BY_NAME, row("id", CRIS, "name", "Cris", "password_hash", STORED));
        mockMvc.perform(login(ADMIN_LOGIN, "Cris"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"))
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));

        assertThat(jdbc.callsWith("jcfit_tokens")).isEmpty();
    }

    @Test
    void bothLogoutsRevokeTheSessionOfTheRequest() throws Exception {
        for (String uri : new String[]{"/jcfit-api/v1/auth/logout", "/jcfit-api/v1/admin/auth/logout"}) {
            String session = JcfitSha256.hex(uri);
            mockMvc.perform(post(uri).requestAttr(JcfitCurrentUser.ATTRIBUTE,
                            new JcfitCurrentUser(MAGOMEZ, "Magomez", session)))
                    .andExpect(status().isNoContent());

            assertThat(jdbc.last().sql()).isEqualTo("DELETE FROM jcfit_tokens WHERE token_hash = ?");
            assertThat(jdbc.last().args()).containsExactly(session);
        }
    }

    private static org.springframework.test.web.servlet.RequestBuilder login(String uri, String username) {
        return post(uri).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}");
    }
}
