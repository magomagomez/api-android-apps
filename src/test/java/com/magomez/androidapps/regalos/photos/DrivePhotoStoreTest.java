package com.magomez.androidapps.regalos.photos;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DrivePhotoStore} against a throwaway in-process server playing Google's token endpoint
 * and Drive: the refresh token exchange and its cache, the multipart upload into the folder,
 * download and delete, and that no secret ever reaches a log.
 */
class DrivePhotoStoreTest {

    private static final String CLIENT_ID = "client-id-de-prueba.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "GOCSPX-secreto-de-prueba";
    private static final String REFRESH_TOKEN = "1//refresh-token-de-prueba";
    private static final String ACCESS_TOKEN = "ya29.access-token-de-prueba";
    private static final String FOLDER = "1iBOhvnJ4ND89PNHCjQgx3BjFR8Lmyftm";
    private static final String FILE_ID = "1AbCdEfGhIjKlMnOpQrStUv";

    private HttpServer server;
    private final List<Map<String, String>> tokenForms = new CopyOnWriteArrayList<>();
    private final List<Seen> seen = new CopyOnWriteArrayList<>();
    private int tokenStatus = 200;
    private int driveStatus = 200;
    private byte[] driveBody = ("{\"id\": \"" + FILE_ID + "\"}").getBytes(StandardCharsets.UTF_8);
    private ListAppender<ILoggingEvent> logs;

    private record Seen(String method, String path, String query, String authorization, String contentType,
                        byte[] body) {
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", this::token);
        server.createContext("/upload/drive/v3/files", this::drive);
        server.createContext("/drive/v3/files", this::drive);
        server.start();
        logs = new ListAppender<>();
        logs.start();
        logger().addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger().detachAppender(logs);
        server.stop(0);
    }

    @Test
    void uploadsTheJpegIntoTheFolderWithAnAccessTokenBoughtWithTheRefreshToken() {
        DrivePhotoStore store = store(FOLDER);
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3};

        assertThat(store.isConfigured()).isTrue();
        assertThat(store.upload("regalos-item-42-a91f.jpg", jpeg)).isEqualTo(FILE_ID);

        assertThat(tokenForms).singleElement().satisfies(form -> assertThat(form)
                .containsEntry("grant_type", "refresh_token")
                .containsEntry("client_id", CLIENT_ID)
                .containsEntry("client_secret", CLIENT_SECRET)
                .containsEntry("refresh_token", REFRESH_TOKEN));
        Seen upload = seen.get(0);
        assertThat(upload.method()).isEqualTo("POST");
        assertThat(upload.path()).isEqualTo("/upload/drive/v3/files");
        assertThat(upload.query()).contains("uploadType=multipart").contains("fields=id");
        assertThat(upload.authorization()).isEqualTo("Bearer " + ACCESS_TOKEN);
        assertThat(upload.contentType()).startsWith("multipart/related");
        String body = new String(upload.body(), StandardCharsets.ISO_8859_1);
        assertThat(body).contains("\"name\":\"regalos-item-42-a91f.jpg\"")
                .contains("\"parents\":[\"" + FOLDER + "\"]")
                .contains("Content-Type: image/jpeg")
                .contains(new String(jpeg, StandardCharsets.ISO_8859_1));
    }

    @Test
    void theAccessTokenIsReusedForLaterCalls() {
        DrivePhotoStore store = store(FOLDER);

        store.upload("a.jpg", new byte[]{1});
        store.delete(FILE_ID);

        assertThat(tokenForms).hasSize(1);
        assertThat(seen).extracting(Seen::authorization).containsOnly("Bearer " + ACCESS_TOKEN);
    }

    @Test
    void aRejectedTokenIsDroppedAndTheNextCallBuysAnother() {
        DrivePhotoStore store = store(FOLDER);
        driveStatus = 401;
        assertThatThrownBy(() -> store.delete(FILE_ID)).isInstanceOf(PhotoStoreException.class);

        driveStatus = 204;
        store.delete(FILE_ID);

        assertThat(tokenForms).hasSize(2);
    }

    @Test
    void downloadsAPhotoAndAPhotoGoneFromDriveIsEmpty() {
        DrivePhotoStore store = store(FOLDER);
        driveBody = new byte[]{9, 8, 7};

        assertThat(store.download(FILE_ID)).hasValueSatisfying(bytes -> assertThat(bytes).containsExactly(9, 8, 7));
        assertThat(seen.get(0).path()).isEqualTo("/drive/v3/files/" + FILE_ID);
        assertThat(seen.get(0).query()).isEqualTo("alt=media");

        driveStatus = 404;
        assertThat(store.download(FILE_ID)).isEmpty();
    }

    @Test
    void deletingAPhotoAlreadyGoneIsFine() {
        DrivePhotoStore store = store(FOLDER);
        driveStatus = 404;

        store.delete(FILE_ID);

        assertThat(seen.get(0).method()).isEqualTo("DELETE");
        assertThat(seen.get(0).path()).isEqualTo("/drive/v3/files/" + FILE_ID);
    }

    @Test
    void aFileIdThatIsNotOneNeverReachesAUrl() {
        DrivePhotoStore store = store(FOLDER);

        assertThatThrownBy(() -> store.download("../../about?x=1")).isInstanceOf(PhotoStoreException.class);
        assertThatThrownBy(() -> store.delete(null)).isInstanceOf(PhotoStoreException.class);
        assertThat(seen).isEmpty();
    }

    @Test
    void driveRefusingTheUploadIsAPhotoStoreError() {
        DrivePhotoStore store = store(FOLDER);
        driveStatus = 403;

        assertThatThrownBy(() -> store.upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);
    }

    @Test
    void googleRefusingTheRefreshTokenIsAPhotoStoreErrorAndNothingReachesDrive() {
        DrivePhotoStore store = store(FOLDER);
        tokenStatus = 400;

        assertThatThrownBy(() -> store.upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);
        assertThat(seen).isEmpty();
    }

    @Test
    void withoutCredentialsOrAValidFolderItIsNotConfigured() {
        assertThat(store("")).extracting(DrivePhotoStore::isConfigured).isEqualTo(false);
        assertThat(store("no es un id")).extracting(DrivePhotoStore::isConfigured).isEqualTo(false);
        DrivePhotoStore noSecret = new DrivePhotoStore(new DrivePhotoStore.Settings(CLIENT_ID, "", REFRESH_TOKEN,
                FOLDER, url("/token"), url("")), new OkHttpClient(), Clock.systemUTC());
        assertThat(noSecret.isConfigured()).isFalse();
        assertThatThrownBy(() -> noSecret.upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);
        assertThat(tokenForms).isEmpty();
    }

    @Test
    void noSecretEverReachesALog() {
        DrivePhotoStore store = store(FOLDER);
        tokenStatus = 400;
        assertThatThrownBy(() -> store.upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);
        tokenStatus = 200;
        driveStatus = 500;
        assertThatThrownBy(() -> store.upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);

        assertThat(logs.list).isNotEmpty();
        for (ILoggingEvent event : logs.list) {
            for (String secret : List.of(CLIENT_SECRET, REFRESH_TOKEN, ACCESS_TOKEN)) {
                assertThat(event.getFormattedMessage()).doesNotContain(secret);
            }
        }
    }

    private DrivePhotoStore store(String folder) {
        return new DrivePhotoStore(new DrivePhotoStore.Settings(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN, folder,
                url("/token"), url("")), new OkHttpClient(),
                Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), ZoneOffset.UTC));
    }

    private String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    private void token(HttpExchange exchange) throws IOException {
        Map<String, String> form = new HashMap<>();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            form.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        tokenForms.add(form);
        respond(exchange, tokenStatus, ("{\"access_token\": \"" + ACCESS_TOKEN + "\", \"expires_in\": 3599}")
                .getBytes(StandardCharsets.UTF_8));
    }

    private void drive(HttpExchange exchange) throws IOException {
        seen.add(new Seen(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getRawQuery(), exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("Content-Type"), exchange.getRequestBody().readAllBytes()));
        respond(exchange, driveStatus, driveStatus == 204 ? null : driveBody);
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        if (body == null) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(DrivePhotoStore.class);
    }
}
