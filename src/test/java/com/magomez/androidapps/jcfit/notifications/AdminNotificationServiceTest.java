package com.magomez.androidapps.jcfit.notifications;

import com.magomez.androidapps.jcfit.auth.JcfitRecordingJdbcTemplate;
import com.magomez.androidapps.jcfit.config.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.assertApiError;
import static com.magomez.androidapps.jcfit.auth.JcfitTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Sending a backoffice notice to every device of the app account, and the list of notices. */
class AdminNotificationServiceTest {

    private static final int CRIS = 2;
    private static final long NEXT_ID = 3;
    private static final String TOKENS = "SELECT token FROM jcfit_devices WHERE user_id = ?";
    private static final String NEXT_VAL = "nextval(pg_get_serial_sequence('jcfit_notifications', 'id'))";
    private static final String INSERT = "INSERT INTO jcfit_notifications";
    private static final String DELETE_DEVICE = "DELETE FROM jcfit_devices WHERE token = ?";
    private static final String RECENT = "FROM jcfit_notifications ORDER BY sent_at DESC";
    private static final OffsetDateTime SENT_AT = OffsetDateTime.of(2026, 10, 19, 8, 0, 0, 0, ZoneOffset.UTC);

    private JcfitRecordingJdbcTemplate jdbc;
    private FakePushSender push;
    private AdminNotificationService service;

    @BeforeEach
    void setUp() {
        jdbc = new JcfitRecordingJdbcTemplate();
        jdbc.scalar(NEXT_VAL, NEXT_ID);
        push = new FakePushSender();
        service = service(push);
    }

    @Test
    void sendsToEveryDeviceOfTheAppAccountAndSavesTheCounts() {
        devices("a", "b");
        saved(2, 0);

        AdminNotification notification = service.send(new AdminNotificationRequest(" ¡A por la semana! ", " Ánimo "));

        assertThat(jdbc.callsWith(TOKENS).get(0).args()).containsExactly(CRIS);
        assertThat(push.pushes).containsExactly(
                new FakePushSender.Push("a", "¡A por la semana!", "Ánimo",
                        Map.of("type", "admin_message", "notificationId", "3")),
                new FakePushSender.Push("b", "¡A por la semana!", "Ánimo",
                        Map.of("type", "admin_message", "notificationId", "3")));
        assertThat(jdbc.callsWith(INSERT).get(0).args()).containsExactly(3L, "¡A por la semana!", "Ánimo", 2, 0);
        assertThat(notification).isEqualTo(
                new AdminNotification(3, "¡A por la semana!", "Ánimo", SENT_AT.toInstant(), 2, 0));
    }

    @Test
    void partialDeliveryIsSavedWithTheFailures() {
        devices("a", "b", "c");
        push.answer("b", JcfitPushResult.FAILED);
        saved(2, 1);

        service.send(new AdminNotificationRequest("Hola", "Texto"));

        assertThat(jdbc.callsWith(INSERT).get(0).args()).containsExactly(3L, "Hola", "Texto", 2, 1);
        assertThat(jdbc.callsWith(DELETE_DEVICE)).isEmpty();
    }

    @Test
    void deadTokensAreDeletedAndCountAsFailed() {
        devices("a", "dead1", "dead2");
        push.answer("dead1", JcfitPushResult.DEAD_TOKEN).answer("dead2", JcfitPushResult.DEAD_TOKEN);
        saved(1, 2);

        service.send(new AdminNotificationRequest("Hola", "Texto"));

        assertThat(jdbc.callsWith(DELETE_DEVICE)).extracting(call -> List.of(call.args()))
                .containsExactly(List.of("dead1"), List.of("dead2"));
        assertThat(jdbc.callsWith(INSERT).get(0).args()).containsExactly(3L, "Hola", "Texto", 1, 2);
    }

    @Test
    void onlyDeadTokensIsStillSavedWithNothingDelivered() {
        devices("dead");
        push.answer("dead", JcfitPushResult.DEAD_TOKEN);
        saved(0, 1);

        AdminNotification notification = service.send(new AdminNotificationRequest("Hola", "Texto"));

        assertThat(notification.delivered()).isZero();
        assertThat(jdbc.callsWith(DELETE_DEVICE)).hasSize(1);
    }

    @Test
    void whenEveryDeviceFailsForAGeneralReasonItIs502AndNothingIsSaved() {
        devices("a", "b");
        push.answer("a", JcfitPushResult.FAILED).answer("b", JcfitPushResult.FAILED);

        ApiException e = assertApiError(() -> service.send(new AdminNotificationRequest("Hola", "Texto")),
                HttpStatus.BAD_GATEWAY, "fcm_error");

        assertThat(e.getMessage()).isEqualTo("No se ha podido enviar el aviso; inténtalo más tarde");
        assertThat(jdbc.callsWith(INSERT)).isEmpty();
        assertThat(jdbc.callsWith(DELETE_DEVICE)).isEmpty();
    }

    @Test
    void generalFailuresMixedWithDeadTokensAreSavedAndTheDeadOnesDeleted() {
        devices("a", "dead");
        push.answer("a", JcfitPushResult.FAILED).answer("dead", JcfitPushResult.DEAD_TOKEN);
        saved(0, 2);

        service.send(new AdminNotificationRequest("Hola", "Texto"));

        assertThat(jdbc.callsWith(DELETE_DEVICE)).hasSize(1);
        assertThat(jdbc.callsWith(INSERT).get(0).args()).containsExactly(3L, "Hola", "Texto", 0, 2);
    }

    @Test
    void withoutDevicesItIsSavedWithNothingDeliveredAndFcmIsNotCalled() {
        saved(0, 0);

        AdminNotification notification = service.send(new AdminNotificationRequest("Hola", "Texto"));

        assertThat(push.pushes).isEmpty();
        assertThat(notification.delivered()).isZero();
        assertThat(jdbc.callsWith(INSERT).get(0).args()).containsExactly(3L, "Hola", "Texto", 0, 0);
    }

    @Test
    void unconfiguredFirebaseIs503AndNothingIsSavedEvenWithoutDevices() {
        AdminNotificationService unconfigured = service(new FakePushSender().unconfigured());

        ApiException e = assertApiError(() -> unconfigured.send(new AdminNotificationRequest("Hola", "Texto")),
                HttpStatus.SERVICE_UNAVAILABLE, "notifications_unavailable");

        assertThat(e.getMessage()).isEqualTo("Las notificaciones todavía no están configuradas");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void titleIs1To60CharactersAfterTrimming() {
        saved(0, 0);
        for (String title : new String[]{null, "", "   ", "t".repeat(61)}) {
            ApiException e = assertApiError(() -> service.send(new AdminNotificationRequest(title, "Texto")),
                    HttpStatus.BAD_REQUEST, "bad_request");
            assertThat(e.getMessage()).isEqualTo("El título es obligatorio y tiene como máximo 60 caracteres");
        }
        service.send(new AdminNotificationRequest("  " + "t".repeat(60) + "  ", "Texto"));
        assertThat(jdbc.callsWith(INSERT).get(0).args()[1]).isEqualTo("t".repeat(60));
    }

    @Test
    void bodyIs1To300CharactersAfterTrimming() {
        saved(0, 0);
        for (String body : new String[]{null, "", "  ", "b".repeat(301)}) {
            ApiException e = assertApiError(() -> service.send(new AdminNotificationRequest("Hola", body)),
                    HttpStatus.BAD_REQUEST, "bad_request");
            assertThat(e.getMessage()).isEqualTo("El texto es obligatorio y tiene como máximo 300 caracteres");
        }
        service.send(new AdminNotificationRequest("Hola", "b".repeat(300)));
        assertThat(jdbc.callsWith(INSERT)).hasSize(1);
    }

    @Test
    void lengthsCountCharactersNotUtf16Units() {
        saved(0, 0);

        service.send(new AdminNotificationRequest("💪".repeat(60), "Texto"));

        assertThat(jdbc.callsWith(INSERT)).hasSize(1);
    }

    @Test
    void missingBodyIs400BeforeAnythingElse() {
        assertApiError(() -> service(new FakePushSender().unconfigured()).send(null), HttpStatus.BAD_REQUEST,
                "bad_request");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void listIsNewestFirstWithTheLimit() {
        jdbc.rows(RECENT, notificationRow(5, 1, 0), notificationRow(4, 0, 1));

        List<AdminNotification> list = service.list(2);

        assertThat(list).extracting(AdminNotification::id).containsExactly(5, 4);
        assertThat(jdbc.last().sql()).isEqualTo(
                "SELECT id, title, body, sent_at, delivered, failed FROM jcfit_notifications "
                        + "ORDER BY sent_at DESC, id DESC LIMIT ?");
        assertThat(jdbc.last().args()).containsExactly(2);
    }

    @Test
    void listDefaultsTo20AndAccepts1To100() {
        service.list(null);
        assertThat(jdbc.last().args()).containsExactly(20);
        service.list(1);
        service.list(100);

        for (int limit : new int[]{0, 101, -1}) {
            ApiException e = assertApiError(() -> service.list(limit), HttpStatus.BAD_REQUEST, "bad_request");
            assertThat(e.getMessage()).isEqualTo("El límite tiene que ir de 1 a 100");
        }
    }

    private void devices(String... tokens) {
        jdbc.rows(TOKENS, java.util.Arrays.stream(tokens).map(token -> row("token", token))
                .toArray(Map[]::new));
    }

    private void saved(int delivered, int failed) {
        jdbc.rows(INSERT, notificationRow((int) NEXT_ID, delivered, failed, "¡A por la semana!", "Ánimo"));
    }

    static Map<String, Object> notificationRow(int id, int delivered, int failed) {
        return notificationRow(id, delivered, failed, "Aviso " + id, "Texto " + id);
    }

    static Map<String, Object> notificationRow(int id, int delivered, int failed, String title, String body) {
        return row("id", id, "title", title, "body", body, "sent_at", SENT_AT, "delivered", delivered,
                "failed", failed);
    }

    private AdminNotificationService service(JcfitPushSender sender) {
        return new AdminNotificationService(new JcfitDeviceDao(jdbc), new JcfitNotificationDao(jdbc), sender, CRIS);
    }
}
