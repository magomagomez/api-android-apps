package com.magomez.androidapps.regalos.photos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The photos live in a Drive folder of a personal account, reached through a small Google Apps
 * Script web app that runs as that account (apps-script/regalos-fotos/Code.gs): a service account
 * has no quota in a personal Drive, and an OAuth app would need publishing. Each call is one POST
 * with a JSON body carrying the shared secret, never in the URL; the script always answers 200 with
 * {"ok": ...}. Neither the secret nor the script URL is ever logged.
 */
@Component
public class AppsScriptPhotoStore implements PhotoStore {

    private static final Logger log = LoggerFactory.getLogger(AppsScriptPhotoStore.class);

    /** Only Google's own web app URLs: the secret goes nowhere else. */
    static final Pattern SCRIPT_URL = Pattern.compile("https://script\\.google\\.com/macros/s/[A-Za-z0-9_-]{20,200}/exec");
    static final int MIN_SECRET_LENGTH = 32;
    /** A stored photo is at most this big; anything larger is not one of ours. */
    static final int MAX_ANSWER_BYTES = 10 * 1024 * 1024;

    /** Apps Script can take a few seconds to wake up. */
    private static final Duration TIMEOUT = Duration.ofSeconds(25);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    /** Drive file ids: letters, digits, '-' and '_'. */
    private static final Pattern FILE_ID = Pattern.compile("[A-Za-z0-9_-]{10,100}");
    private static final String NOT_FOUND = "not_found";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpUrl url;
    private final String secret;
    private final OkHttpClient http;

    @Autowired
    public AppsScriptPhotoStore(@Value("${regalos.photos.script-url:}") String scriptUrl,
                                @Value("${regalos.photos.script-secret:}") String secret) {
        this(SCRIPT_URL.matcher(scriptUrl == null ? "" : scriptUrl).matches() ? scriptUrl : null, secret,
                new OkHttpClient.Builder().callTimeout(TIMEOUT).build());
    }

    /** scriptUrl is already checked (or a local test server); null leaves the store unconfigured. */
    AppsScriptPhotoStore(String scriptUrl, String secret, OkHttpClient http) {
        this.url = scriptUrl == null ? null : HttpUrl.get(scriptUrl);
        this.secret = secret == null ? "" : secret;
        // Apps Script answers a POST with a 302 to the result, which must be fetched with GET.
        this.http = http.newBuilder().followRedirects(true).build();
    }

    @Override
    public boolean isConfigured() {
        return url != null && secret.length() >= MIN_SECRET_LENGTH;
    }

    @Override
    public String upload(String name, byte[] jpeg) {
        ObjectNode request = request("upload").put("name", name).put("data", Base64.getEncoder().encodeToString(jpeg));
        JsonNode answer = call(request, "upload");
        if (!answer.path("ok").asBoolean(false)) {
            throw refused("upload", answer);
        }
        String id = answer.path("id").asText("");
        if (!FILE_ID.matcher(id).matches()) {
            throw new PhotoStoreException("The photo script answered an upload without a file id");
        }
        return id;
    }

    @Override
    public Optional<byte[]> download(String fileId) {
        JsonNode answer = call(request("download").put("fileId", checked(fileId)), "download");
        if (!answer.path("ok").asBoolean(false)) {
            if (NOT_FOUND.equals(answer.path("error").asText())) {
                return Optional.empty();
            }
            throw refused("download", answer);
        }
        try {
            return Optional.of(Base64.getDecoder().decode(answer.path("data").asText("")));
        } catch (IllegalArgumentException e) {
            throw new PhotoStoreException("The photo script answered a download that is not base64");
        }
    }

    @Override
    public void delete(String fileId) {
        JsonNode answer = call(request("delete").put("fileId", checked(fileId)), "delete");
        if (!answer.path("ok").asBoolean(false) && !NOT_FOUND.equals(answer.path("error").asText())) {
            throw refused("delete", answer);
        }
    }

    private ObjectNode request(String action) {
        return mapper.createObjectNode().put("secret", secret).put("action", action);
    }

    private JsonNode call(ObjectNode body, String what) {
        if (!isConfigured()) {
            throw new PhotoStoreException("The photo script is not configured");
        }
        Request request = new Request.Builder().url(url).post(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("regalos: the photo script failed a {} (HTTP {})", what, response.code());
                throw new PhotoStoreException("The photo script failed a " + what);
            }
            return readJson(response.body());
        } catch (IOException e) {
            log.warn("regalos: could not reach the photo script ({})", e.getClass().getSimpleName());
            throw new PhotoStoreException("The photo script could not be reached");
        }
    }

    private static String checked(String fileId) {
        if (fileId == null || !FILE_ID.matcher(fileId).matches()) {
            throw new PhotoStoreException("Not a Drive file id");
        }
        return fileId;
    }

    /** Logs only the script's own error code, one of a few known words. */
    private static PhotoStoreException refused(String what, JsonNode answer) {
        String error = answer.path("error").asText("");
        log.warn("regalos: the photo script refused a {} ({})", what, error.matches("[a-z_]{1,20}") ? error : "?");
        return new PhotoStoreException("The photo script refused the " + what);
    }

    private JsonNode readJson(ResponseBody body) throws IOException {
        if (body == null) {
            return mapper.createObjectNode();
        }
        try (InputStream in = body.byteStream()) {
            byte[] bytes = in.readNBytes(MAX_ANSWER_BYTES + 1);
            if (bytes.length > MAX_ANSWER_BYTES) {
                throw new PhotoStoreException("The photo script answered more than any photo we save");
            }
            JsonNode node = mapper.readTree(bytes);
            return node == null ? mapper.createObjectNode() : node;
        } catch (JsonProcessingException e) {
            // Not JSON: a Google sign-in or error page, e.g. when the web app is not shared with anyone.
            log.warn("regalos: the photo script did not answer JSON (is it deployed for anyone?)");
            return mapper.createObjectNode();
        }
    }
}
