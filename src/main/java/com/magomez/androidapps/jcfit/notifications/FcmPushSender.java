package com.magomez.androidapps.jcfit.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Firebase Cloud Messaging HTTP v1 without the Firebase SDK: a JWT signed with the service
 * account key (RS256) buys an OAuth access token, cached until shortly before it expires, and
 * each message is one POST to messages:send. Neither the key, the JWT, the access token nor the
 * device tokens are ever logged.
 */
@Component
public class FcmPushSender implements JcfitPushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    static final String DEFAULT_FCM_URL = "https://fcm.googleapis.com";
    static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    static final String CHANNEL_ID = "avisos";
    static final Duration TOKEN_LIFETIME = Duration.ofHours(1);
    /** An access token this close to expiring is renewed instead of used. */
    static final Duration REFRESH_MARGIN = Duration.ofMinutes(5);

    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final String JWT_HEADER = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String UNREGISTERED = "UNREGISTERED";
    private static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
    private static final String TOKEN_FIELD = "message.token";
    private static final String TOKEN_ERROR_HINT = "registration token";
    private static final int HTTP_UNAUTHORIZED = 401;
    /** Error statuses of FCM are enum names; anything else is not logged. */
    private static final Pattern LOGGABLE_STATUS = Pattern.compile("[A-Z_]{1,40}");

    private final ObjectMapper mapper = new ObjectMapper();
    private final FcmServiceAccount account;
    private final HttpUrl sendUrl;
    private final OkHttpClient http;
    private final Clock clock;

    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.MIN;

    @Autowired
    public FcmPushSender(@Value("${jcfit.firebase.service-account:}") String serviceAccount,
                         @Value("${jcfit.firebase.fcm-url:" + DEFAULT_FCM_URL + "}") String fcmUrl) {
        this(serviceAccount, fcmUrl, new OkHttpClient.Builder().callTimeout(TIMEOUT).build(), Clock.systemUTC());
    }

    FcmPushSender(String serviceAccount, String fcmUrl, OkHttpClient http, Clock clock) {
        this.account = account(serviceAccount);
        this.sendUrl = account == null ? null : HttpUrl.get(fcmUrl).newBuilder()
                .addPathSegment("v1").addPathSegment("projects").addPathSegment(account.projectId())
                .addPathSegment("messages:send").build();
        this.http = http;
        this.clock = clock;
    }

    @Override
    public boolean isConfigured() {
        return account != null;
    }

    @Override
    public JcfitPushResult send(String deviceToken, String title, String body, Map<String, String> data) {
        if (account == null) {
            return JcfitPushResult.FAILED;
        }
        String bearer = accessToken();
        if (bearer == null) {
            return JcfitPushResult.FAILED;
        }
        Request request = new Request.Builder()
                .url(sendUrl)
                .header("Authorization", "Bearer " + bearer)
                .post(RequestBody.create(message(deviceToken, title, body, data), JSON))
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return JcfitPushResult.DELIVERED;
            }
            return failure(response.code(), errorOf(response.body()));
        } catch (IOException e) {
            log.warn("jcfit: could not reach FCM ({})", e.getClass().getSimpleName());
            return JcfitPushResult.FAILED;
        }
    }

    private JcfitPushResult failure(int code, JsonNode error) {
        String status = error.path("status").asText("");
        boolean unregistered = false;
        boolean tokenField = false;
        for (JsonNode detail : error.path("details")) {
            unregistered |= UNREGISTERED.equals(detail.path("errorCode").asText());
            for (JsonNode violation : detail.path("fieldViolations")) {
                tokenField |= TOKEN_FIELD.equals(violation.path("field").asText());
            }
        }
        boolean aboutToken = tokenField
                || error.path("message").asText("").toLowerCase(Locale.ROOT).contains(TOKEN_ERROR_HINT);
        if (unregistered || (INVALID_ARGUMENT.equals(status) && aboutToken)) {
            log.info("jcfit: FCM says a device token is no longer valid (HTTP {})", code);
            return JcfitPushResult.DEAD_TOKEN;
        }
        if (code == HTTP_UNAUTHORIZED) {
            dropAccessToken();
        }
        log.warn("jcfit: FCM refused a message (HTTP {} {})", code,
                LOGGABLE_STATUS.matcher(status).matches() ? status : "");
        return JcfitPushResult.FAILED;
    }

    /** The cached access token, or a new one; null when Google does not give one. */
    private synchronized String accessToken() {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenExpiresAt.minus(REFRESH_MARGIN))) {
            return accessToken;
        }
        String assertion;
        try {
            assertion = jwt(now);
        } catch (GeneralSecurityException e) {
            log.warn("jcfit: could not sign the Firebase JWT ({})", e.getClass().getSimpleName());
            return null;
        }
        Request request = new Request.Builder()
                .url(account.tokenUri())
                .post(new FormBody.Builder().add("grant_type", GRANT_TYPE).add("assertion", assertion).build())
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("jcfit: Google refused the Firebase service account (HTTP {})", response.code());
                return null;
            }
            JsonNode answer = readJson(response.body());
            String token = answer.path("access_token").asText("");
            if (token.isEmpty()) {
                log.warn("jcfit: Google answered without an access token");
                return null;
            }
            long seconds = answer.path("expires_in").asLong(TOKEN_LIFETIME.toSeconds());
            accessToken = token;
            accessTokenExpiresAt = now.plusSeconds(seconds);
            return token;
        } catch (IOException e) {
            log.warn("jcfit: could not reach Google for a Firebase access token ({})", e.getClass().getSimpleName());
            return null;
        }
    }

    private synchronized void dropAccessToken() {
        accessToken = null;
        accessTokenExpiresAt = Instant.MIN;
    }

    private String jwt(Instant now) throws GeneralSecurityException {
        ObjectNode claims = mapper.createObjectNode()
                .put("iss", account.clientEmail())
                .put("scope", SCOPE)
                .put("aud", account.tokenUri())
                .put("iat", now.getEpochSecond())
                .put("exp", now.plus(TOKEN_LIFETIME).getEpochSecond());
        Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
        String unsigned = base64.encodeToString(JWT_HEADER.getBytes(StandardCharsets.UTF_8)) + "."
                + base64.encodeToString(claims.toString().getBytes(StandardCharsets.UTF_8));
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(account.privateKey());
        signature.update(unsigned.getBytes(StandardCharsets.US_ASCII));
        return unsigned + "." + base64.encodeToString(signature.sign());
    }

    private String message(String deviceToken, String title, String body, Map<String, String> data) {
        ObjectNode message = mapper.createObjectNode();
        ObjectNode content = message.putObject("message");
        content.put("token", deviceToken);
        content.putObject("notification").put("title", title).put("body", body);
        ObjectNode dataNode = content.putObject("data");
        data.forEach(dataNode::put);
        ObjectNode android = content.putObject("android");
        android.put("priority", "high");
        android.putObject("notification").put("channel_id", CHANNEL_ID);
        return message.toString();
    }

    /** The "error" object of an FCM error answer; an empty node when there is none. */
    private JsonNode errorOf(ResponseBody body) {
        return readJson(body).path("error");
    }

    private JsonNode readJson(ResponseBody body) {
        if (body == null) {
            return mapper.createObjectNode();
        }
        try {
            JsonNode node = mapper.readTree(body.string());
            return node == null ? mapper.createObjectNode() : node;
        } catch (IOException e) {
            // Not JSON (an HTML error page, say) or cut short: treated as an answer without details.
            return mapper.createObjectNode();
        }
    }

    private static FcmServiceAccount account(String serviceAccount) {
        try {
            FcmServiceAccount parsed = FcmServiceAccount.parse(serviceAccount).orElse(null);
            if (parsed == null) {
                log.warn("jcfit: JCFIT_FIREBASE_SERVICE_ACCOUNT is not set, the backoffice cannot send notices");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            // The message is safe by construction: it never carries the value.
            log.warn("jcfit: JCFIT_FIREBASE_SERVICE_ACCOUNT is not valid ({}), the backoffice cannot send notices",
                    e.getMessage());
            return null;
        }
    }
}
