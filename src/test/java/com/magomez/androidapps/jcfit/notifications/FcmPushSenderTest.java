package com.magomez.androidapps.jcfit.notifications;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FcmPushSender} against a throwaway in-process HTTP server playing both Google's token
 * endpoint and FCM: the signed JWT, the access token cache, the message body, dead tokens vs
 * general failures, and that no secret ever reaches a log.
 */
class FcmPushSenderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ACCESS_TOKEN = "ya29.access-token-de-prueba";
    private static final String SECOND_ACCESS_TOKEN = "ya29.segundo-access-token";
    private static final String DEVICE = "device-token-de-cris";
    private static final String SEND_PATH = "/v1/projects/" + FcmTestAccounts.PROJECT_ID + "/messages:send";
    private static final Map<String, String> DATA = Map.of("type", "admin_message", "notificationId", "3");
    private static final String UNREGISTERED = """
            {"error": {"code": 404, "message": "Requested entity was not found.", "status": "NOT_FOUND",
              "details": [{"@type": "type.googleapis.com/google.firebase.fcm.v1.FcmError", "errorCode": "UNREGISTERED"}]}}
            """;
    private static final String INVALID_TOKEN = """
            {"error": {"code": 400, "message": "The registration token is not a valid FCM registration token",
              "status": "INVALID_ARGUMENT",
              "details": [{"@type": "type.googleapis.com/google.firebase.fcm.v1.FcmError", "errorCode": "INVALID_ARGUMENT"},
                {"@type": "type.googleapis.com/google.rpc.BadRequest",
                 "fieldViolations": [{"field": "message.token", "description": "Invalid registration token"}]}]}}
            """;
    private static final String INVALID_MESSAGE = """
            {"error": {"code": 400, "message": "Invalid value at 'message.android.priority'",
              "status": "INVALID_ARGUMENT",
              "details": [{"@type": "type.googleapis.com/google.rpc.BadRequest",
                 "fieldViolations": [{"field": "message.android.priority", "description": "Invalid value"}]}]}}
            """;

    private HttpServer server;
    private final List<String> assertions = new CopyOnWriteArrayList<>();
    private final List<Map<String, String>> tokenForms = new CopyOnWriteArrayList<>();
    private final List<JsonNode> messages = new CopyOnWriteArrayList<>();
    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private final List<String> sendPaths = new CopyOnWriteArrayList<>();
    private final Map<String, Answer> fcmAnswers = new ConcurrentHashMap<>();
    private final List<Answer> tokenAnswers = new CopyOnWriteArrayList<>();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-19T08:00:00Z"));
    private ListAppender<ILoggingEvent> logs;

    private record Answer(int status, String body) {
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", this::token);
        server.createContext("/v1/projects/", this::send);
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
    void exchangesASignedJwtForAnAccessTokenAndSendsTheMessage() throws Exception {
        FcmPushSender sender = sender();

        assertThat(sender.isConfigured()).isTrue();
        assertThat(sender.send(DEVICE, "¡A por la semana!", "Ánimo", DATA)).isEqualTo(JcfitPushResult.DELIVERED);

        assertThat(tokenForms).hasSize(1);
        assertThat(tokenForms.get(0)).containsEntry("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        String[] jwt = assertions.get(0).split("\\.");
        assertThat(jwt).hasSize(3);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(FcmTestAccounts.KEYS.getPublic());
        verifier.update((jwt[0] + "." + jwt[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(Base64.getUrlDecoder().decode(jwt[2]))).as("signature").isTrue();
        JsonNode header = MAPPER.readTree(Base64.getUrlDecoder().decode(jwt[0]));
        assertThat(header.get("alg").asText()).isEqualTo("RS256");
        assertThat(header.get("typ").asText()).isEqualTo("JWT");
        JsonNode claims = MAPPER.readTree(Base64.getUrlDecoder().decode(jwt[1]));
        assertThat(claims.get("iss").asText()).isEqualTo(FcmTestAccounts.CLIENT_EMAIL);
        assertThat(claims.get("scope").asText()).isEqualTo("https://www.googleapis.com/auth/firebase.messaging");
        assertThat(claims.get("aud").asText()).isEqualTo(tokenUri());
        assertThat(claims.get("iat").asLong()).isEqualTo(clock.instant().getEpochSecond());
        assertThat(claims.get("exp").asLong()).isEqualTo(clock.instant().getEpochSecond() + 3600);

        assertThat(sendPaths).containsExactly(SEND_PATH);
        assertThat(authorizations).containsExactly("Bearer " + ACCESS_TOKEN);
        assertThat(messages.get(0)).isEqualTo(MAPPER.readTree("""
                {"message": {
                  "token": "device-token-de-cris",
                  "notification": {"title": "¡A por la semana!", "body": "Ánimo"},
                  "data": {"type": "admin_message", "notificationId": "3"},
                  "android": {"priority": "high", "notification": {"channel_id": "avisos"}}
                }}
                """));
    }

    @Test
    void theAccessTokenIsReusedUntilShortlyBeforeItExpires() {
        tokenAnswers.add(new Answer(200, accessToken(ACCESS_TOKEN, 3600)));
        tokenAnswers.add(new Answer(200, accessToken(SECOND_ACCESS_TOKEN, 3600)));
        FcmPushSender sender = sender();

        sender.send(DEVICE, "t", "b", DATA);
        clock.advance(Duration.ofMinutes(50));
        sender.send(DEVICE, "t", "b", DATA);
        assertThat(tokenForms).hasSize(1);

        clock.advance(Duration.ofMinutes(6));
        sender.send(DEVICE, "t", "b", DATA);

        assertThat(tokenForms).hasSize(2);
        assertThat(authorizations).containsExactly("Bearer " + ACCESS_TOKEN, "Bearer " + ACCESS_TOKEN,
                "Bearer " + SECOND_ACCESS_TOKEN);
    }

    @Test
    void unregisteredTokenIsDead() {
        fcmAnswers.put(DEVICE, new Answer(404, UNREGISTERED));

        assertThat(sender().send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.DEAD_TOKEN);
    }

    @Test
    void invalidRegistrationTokenIsDead() {
        fcmAnswers.put(DEVICE, new Answer(400, INVALID_TOKEN));

        assertThat(sender().send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.DEAD_TOKEN);
    }

    /** A bad message would "kill" every device: it is a general failure, not a dead token. */
    @Test
    void invalidArgumentThatIsNotAboutTheTokenIsAGeneralFailure() {
        fcmAnswers.put(DEVICE, new Answer(400, INVALID_MESSAGE));

        assertThat(sender().send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.FAILED);
    }

    @Test
    void notFoundWithoutUnregisteredIsAGeneralFailure() {
        fcmAnswers.put(DEVICE, new Answer(404, "<html>Not Found</html>"));

        assertThat(sender().send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.FAILED);
    }

    @Test
    void authorizationAndServerErrorsAreGeneralFailures() {
        for (int status : new int[]{401, 403, 429, 500, 503}) {
            fcmAnswers.put(DEVICE, new Answer(status, "{\"error\": {\"code\": " + status + ", \"status\": \"X\"}}"));
            assertThat(sender().send(DEVICE, "t", "b", DATA)).as("HTTP " + status).isEqualTo(JcfitPushResult.FAILED);
        }
    }

    @Test
    void a401DropsTheCachedAccessToken() {
        tokenAnswers.add(new Answer(200, accessToken(ACCESS_TOKEN, 3600)));
        tokenAnswers.add(new Answer(200, accessToken(SECOND_ACCESS_TOKEN, 3600)));
        FcmPushSender sender = sender();
        fcmAnswers.put(DEVICE, new Answer(401, "{\"error\": {\"code\": 401, \"status\": \"UNAUTHENTICATED\"}}"));

        assertThat(sender.send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.FAILED);
        fcmAnswers.remove(DEVICE);
        assertThat(sender.send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.DELIVERED);

        assertThat(authorizations).containsExactly("Bearer " + ACCESS_TOKEN, "Bearer " + SECOND_ACCESS_TOKEN);
    }

    @Test
    void aRefusedServiceAccountIsAGeneralFailureAndNothingIsSent() {
        tokenAnswers.add(new Answer(400, "{\"error\": \"invalid_grant\", \"error_description\": \"Invalid JWT Signature.\"}"));
        FcmPushSender sender = sender();

        assertThat(sender.send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.FAILED);
        assertThat(messages).isEmpty();
    }

    @Test
    void aTokenAnswerWithoutAccessTokenIsAGeneralFailure() {
        tokenAnswers.add(new Answer(200, "{\"token_type\": \"Bearer\"}"));

        assertThat(sender().send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.FAILED);
        assertThat(messages).isEmpty();
    }

    @Test
    void unreachableFcmIsAGeneralFailure() {
        FcmPushSender sender = new FcmPushSender(FcmTestAccounts.json(tokenUri()), "http://localhost:1", http(),
                clock);

        assertThat(sender.send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.FAILED);
    }

    @Test
    void unreachableTokenEndpointIsAGeneralFailure() {
        FcmPushSender sender = new FcmPushSender(FcmTestAccounts.json("http://localhost:1/token"), fcmUrl(), http(),
                clock);

        assertThat(sender.send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.FAILED);
        assertThat(messages).isEmpty();
    }

    @Test
    void theAccountMayComeInBase64() {
        FcmPushSender sender = new FcmPushSender(FcmTestAccounts.base64(FcmTestAccounts.json(tokenUri())), fcmUrl(),
                http(), clock);

        assertThat(sender.send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.DELIVERED);
    }

    @Test
    void withoutAccountItIsNotConfiguredSendsNothingAndWarnsOnce() {
        FcmPushSender sender = new FcmPushSender("", fcmUrl(), http(), clock);

        assertThat(sender.isConfigured()).isFalse();
        assertThat(sender.send(DEVICE, "t", "b", DATA)).isEqualTo(JcfitPushResult.FAILED);
        assertThat(sender.isConfigured()).isFalse();
        assertThat(tokenForms).isEmpty();
        assertThat(messages).isEmpty();
        assertThat(logs.list).hasSize(1);
        assertThat(logs.list.get(0).getFormattedMessage()).contains("JCFIT_FIREBASE_SERVICE_ACCOUNT");
    }

    @Test
    void anInvalidAccountIsNotConfiguredAndTheWarningDoesNotRepeatIt() {
        String broken = "{\"project_id\": \"x\", \"private_key\": \"" + FcmTestAccounts.KEY_FRAGMENT + "\"}";
        FcmPushSender sender = new FcmPushSender(broken, fcmUrl(), http(), clock);

        assertThat(sender.isConfigured()).isFalse();
        assertThat(logs.list).hasSize(1);
        assertThat(rendered(logs.list.get(0))).contains("JCFIT_FIREBASE_SERVICE_ACCOUNT")
                .doesNotContain(FcmTestAccounts.KEY_FRAGMENT);
    }

    @Test
    void noSecretEverReachesTheLogs() {
        String echo = "{\"error\": \"invalid_grant\", \"error_description\": \"%s %s\"}";
        server.removeContext("/token");
        server.createContext("/token", exchange -> {
            Map<String, String> form = form(exchange);
            assertions.add(form.get("assertion"));
            reply(exchange, 400, String.format(echo, form.get("assertion"), FcmTestAccounts.KEY_FRAGMENT));
        });
        FcmPushSender refused = sender();
        refused.send(DEVICE, "t", "b", DATA);

        FcmPushSender accepted = new FcmPushSender(FcmTestAccounts.json(tokenUri()), fcmUrl(), http(), clock);
        server.removeContext("/token");
        server.createContext("/token", this::token);
        for (Answer answer : new Answer[]{new Answer(404, UNREGISTERED), new Answer(400, INVALID_TOKEN),
                new Answer(400, INVALID_MESSAGE.replace("message.android.priority", DEVICE + " " + ACCESS_TOKEN)),
                new Answer(401, "{\"error\": {\"message\": \"" + ACCESS_TOKEN + "\"}}"),
                new Answer(500, DEVICE + ACCESS_TOKEN)}) {
            fcmAnswers.put(DEVICE, answer);
            accepted.send(DEVICE, "t", "b", DATA);
        }
        new FcmPushSender(FcmTestAccounts.json(tokenUri()), "http://localhost:1", http(), clock)
                .send(DEVICE, "t", "b", DATA);

        assertThat(logs.list).isNotEmpty();
        List<String> secrets = new ArrayList<>(List.of(FcmTestAccounts.KEY_FRAGMENT, ACCESS_TOKEN, DEVICE));
        secrets.addAll(assertions);
        for (ILoggingEvent event : logs.list) {
            String line = rendered(event);
            for (String secret : secrets) {
                assertThat(line).doesNotContain(secret);
            }
        }
    }

    private FcmPushSender sender() {
        return new FcmPushSender(FcmTestAccounts.json(tokenUri()), fcmUrl(), http(), clock);
    }

    private String tokenUri() {
        return "http://localhost:" + server.getAddress().getPort() + "/token";
    }

    private String fcmUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static OkHttpClient http() {
        return new OkHttpClient.Builder().callTimeout(Duration.ofSeconds(5)).build();
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(FcmPushSender.class);
    }

    private static String rendered(ILoggingEvent event) {
        String throwable = event.getThrowableProxy() == null ? "" : ThrowableProxyUtil.asString(event.getThrowableProxy());
        return event.getFormattedMessage() + throwable;
    }

    private void token(HttpExchange exchange) throws IOException {
        Map<String, String> form = form(exchange);
        tokenForms.add(form);
        assertions.add(form.get("assertion"));
        Answer answer = tokenAnswers.isEmpty() ? new Answer(200, accessToken(ACCESS_TOKEN, 3599))
                : tokenAnswers.get(Math.min(tokenForms.size(), tokenAnswers.size()) - 1);
        reply(exchange, answer.status(), answer.body());
    }

    private void send(HttpExchange exchange) throws IOException {
        sendPaths.add(exchange.getRequestURI().getPath());
        authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
        JsonNode message = MAPPER.readTree(exchange.getRequestBody());
        messages.add(message);
        Answer answer = fcmAnswers.getOrDefault(message.path("message").path("token").asText(),
                new Answer(200, "{\"name\": \"projects/demo-project/messages/0:1\"}"));
        reply(exchange, answer.status(), answer.body());
    }

    private static String accessToken(String token, int expiresIn) {
        return "{\"access_token\": \"" + token + "\", \"expires_in\": " + expiresIn + ", \"token_type\": \"Bearer\"}";
    }

    private static Map<String, String> form(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            form.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return form;
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
