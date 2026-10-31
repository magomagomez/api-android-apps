package com.magomez.androidapps.regalos.photos;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AppsScriptPhotoStore} against a throwaway in-process server playing the Apps Script web
 * app, 302 to the result included: the JSON protocol, the secret only in the body, errors, and
 * that the secret never reaches a log.
 */
class AppsScriptPhotoStoreTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SECRET = "secreto-de-prueba-de-mas-de-32-caracteres";
    private static final String FILE_ID = "1AbCdEfGhIjKlMnOpQrStUv";

    private HttpServer server;
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final List<String> requestLines = new CopyOnWriteArrayList<>();
    private volatile String answer = "{\"ok\": true, \"id\": \"" + FILE_ID + "\"}";
    private volatile int status = 200;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/macros/s/script/exec", this::exec);
        server.createContext("/echo", this::echo);
        server.start();
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(AppsScriptPhotoStore.class)).addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(AppsScriptPhotoStore.class)).detachAppender(logs);
        server.stop(0);
    }

    @Test
    void uploadsTheJpegAsBase64WithTheSecretInTheBodyAndFollowsGooglesRedirect() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3};

        assertThat(store().upload("regalos-item-42-a91f.jpg", jpeg)).isEqualTo(FILE_ID);

        JsonNode request = requests.get(0);
        assertThat(request.get("secret").asText()).isEqualTo(SECRET);
        assertThat(request.get("action").asText()).isEqualTo("upload");
        assertThat(request.get("name").asText()).isEqualTo("regalos-item-42-a91f.jpg");
        assertThat(Base64.getDecoder().decode(request.get("data").asText())).isEqualTo(jpeg);
        assertThat(requestLines).containsExactly("POST /macros/s/script/exec", "GET /echo");
    }

    @Test
    void downloadsAPhotoAndOneNotInTheFolderIsEmpty() {
        answer = "{\"ok\": true, \"data\": \"" + Base64.getEncoder().encodeToString(new byte[]{9, 8, 7}) + "\"}";
        assertThat(store().download(FILE_ID)).hasValueSatisfying(bytes -> assertThat(bytes).containsExactly(9, 8, 7));
        assertThat(requests.get(0).get("action").asText()).isEqualTo("download");
        assertThat(requests.get(0).get("fileId").asText()).isEqualTo(FILE_ID);

        answer = "{\"ok\": false, \"error\": \"not_found\"}";
        assertThat(store().download(FILE_ID)).isEmpty();
    }

    @Test
    void deletingAPhotoAlreadyGoneIsFine() {
        answer = "{\"ok\": false, \"error\": \"not_found\"}";

        store().delete(FILE_ID);

        assertThat(requests.get(0).get("action").asText()).isEqualTo("delete");
    }

    @Test
    void aRefusalOrAFailureIsAPhotoStoreError() {
        answer = "{\"ok\": false, \"error\": \"forbidden\"}";
        assertThatThrownBy(() -> store().upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);
        assertThatThrownBy(() -> store().delete(FILE_ID)).isInstanceOf(PhotoStoreException.class);

        answer = "{\"ok\": true}";
        assertThatThrownBy(() -> store().upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);

        answer = "<html>Inicia sesión</html>";
        assertThatThrownBy(() -> store().download(FILE_ID)).isInstanceOf(PhotoStoreException.class);

        status = 500;
        assertThatThrownBy(() -> store().upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);
    }

    @Test
    void aFileIdThatIsNotOneNeverLeaves() {
        assertThatThrownBy(() -> store().download("../otra-carpeta")).isInstanceOf(PhotoStoreException.class);
        assertThatThrownBy(() -> store().delete(null)).isInstanceOf(PhotoStoreException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void onlyAGoogleScriptUrlAndALongSecretConfigureIt() {
        assertThat(new AppsScriptPhotoStore("https://script.google.com/macros/s/AKfycbx_abcdefghijklmnopqrstuvwxyz/exec",
                SECRET).isConfigured()).isTrue();
        assertThat(new AppsScriptPhotoStore("https://script.google.com/macros/s/AKfycbx_abcdefghijklmnopqrstuvwxyz/exec",
                "corto").isConfigured()).isFalse();
        assertThat(new AppsScriptPhotoStore("https://evil.example.com/macros/s/AKfycbx_abcdefghijklmnop/exec",
                SECRET).isConfigured()).isFalse();
        assertThat(new AppsScriptPhotoStore("", "").isConfigured()).isFalse();

        AppsScriptPhotoStore unset = new AppsScriptPhotoStore(null, SECRET, new OkHttpClient());
        assertThatThrownBy(() -> unset.upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void theSecretNeverReachesALog() {
        answer = "{\"ok\": false, \"error\": \"forbidden " + SECRET + "\"}";
        assertThatThrownBy(() -> store().upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);
        status = 500;
        assertThatThrownBy(() -> store().upload("a.jpg", new byte[]{1})).isInstanceOf(PhotoStoreException.class);

        assertThat(logs.list).isNotEmpty();
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(line -> line.contains(SECRET) || line.contains("localhost"));
    }

    private AppsScriptPhotoStore store() {
        return new AppsScriptPhotoStore("http://localhost:" + server.getAddress().getPort() + "/macros/s/script/exec",
                SECRET, new OkHttpClient());
    }

    /** Like Apps Script: the POST runs the script and answers 302 to where the result waits. */
    private void exec(HttpExchange exchange) throws IOException {
        requestLines.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
        requests.add(MAPPER.readTree(exchange.getRequestBody().readAllBytes()));
        if (status != 200) {
            respond(exchange, status, "error");
            return;
        }
        exchange.getResponseHeaders().add("Location", "/echo");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void echo(HttpExchange exchange) throws IOException {
        requestLines.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
        respond(exchange, 200, answer);
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
