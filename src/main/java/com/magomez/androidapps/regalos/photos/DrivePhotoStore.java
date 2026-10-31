package com.magomez.androidapps.regalos.photos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Google Drive v3 over plain REST, without the Google SDK. A service account cannot own files in
 * a personal Drive, so the owner of the folder authorised this app once and its refresh token
 * buys access tokens, cached until shortly before they expire. Neither the client secret, the
 * refresh token nor an access token is ever logged.
 */
@Component
public class DrivePhotoStore implements PhotoStore {

    private static final Logger log = LoggerFactory.getLogger(DrivePhotoStore.class);

    static final String DEFAULT_TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String DEFAULT_API_URL = "https://www.googleapis.com";
    /** An access token this close to expiring is renewed instead of used. */
    static final Duration REFRESH_MARGIN = Duration.ofMinutes(5);
    /** A stored photo is at most this big; anything larger is not one of ours. */
    static final int MAX_DOWNLOAD_BYTES = 6 * 1024 * 1024;

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final Duration DEFAULT_LIFETIME = Duration.ofHours(1);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType JPEG = MediaType.get("image/jpeg");
    private static final MediaType RELATED = MediaType.get("multipart/related");
    /** Drive file ids: letters, digits, '-' and '_'. Checked before an id goes into a URL. */
    private static final Pattern FILE_ID = Pattern.compile("[A-Za-z0-9_-]{10,100}");
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_UNAUTHORIZED = 401;

    private final ObjectMapper mapper = new ObjectMapper();
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String folderId;
    private final HttpUrl tokenUrl;
    private final HttpUrl apiUrl;
    private final OkHttpClient http;
    private final Clock clock;

    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.MIN;

    @Autowired
    public DrivePhotoStore(@Value("${regalos.drive.client-id:}") String clientId,
                           @Value("${regalos.drive.client-secret:}") String clientSecret,
                           @Value("${regalos.drive.refresh-token:}") String refreshToken,
                           @Value("${regalos.drive.folder-id:}") String folderId) {
        this(new Settings(clientId, clientSecret, refreshToken, folderId, DEFAULT_TOKEN_URL, DEFAULT_API_URL),
                new OkHttpClient.Builder().callTimeout(TIMEOUT).build(), Clock.systemUTC());
    }

    DrivePhotoStore(Settings settings, OkHttpClient http, Clock clock) {
        this.clientId = settings.clientId();
        this.clientSecret = settings.clientSecret();
        this.refreshToken = settings.refreshToken();
        this.folderId = settings.folderId();
        this.tokenUrl = HttpUrl.get(settings.tokenUrl());
        this.apiUrl = HttpUrl.get(settings.apiUrl());
        this.http = http;
        this.clock = clock;
    }

    @Override
    public boolean isConfigured() {
        return !blank(clientId) && !blank(clientSecret) && !blank(refreshToken)
                && folderId != null && FILE_ID.matcher(folderId).matches();
    }

    @Override
    public String upload(String name, byte[] jpeg) {
        String metadata = mapper.createObjectNode()
                .put("name", name)
                .put("mimeType", JPEG.toString())
                .set("parents", mapper.valueToTree(List.of(folderId)))
                .toString();
        RequestBody body = new MultipartBody.Builder()
                .setType(RELATED)
                .addPart(RequestBody.create(metadata, JSON))
                .addPart(RequestBody.create(jpeg, JPEG))
                .build();
        HttpUrl url = apiUrl.newBuilder().addPathSegments("upload/drive/v3/files")
                .addQueryParameter("uploadType", "multipart").addQueryParameter("fields", "id").build();
        try (Response response = call(new Request.Builder().url(url).post(body))) {
            if (!response.isSuccessful()) {
                throw refused("upload", response.code());
            }
            String id = readJson(response.body()).path("id").asText("");
            if (!FILE_ID.matcher(id).matches()) {
                throw new PhotoStoreException("Drive answered an upload without a file id");
            }
            return id;
        }
    }

    @Override
    public Optional<byte[]> download(String fileId) {
        HttpUrl url = fileUrl(fileId).newBuilder().addQueryParameter("alt", "media").build();
        try (Response response = call(new Request.Builder().url(url).get())) {
            if (response.code() == HTTP_NOT_FOUND) {
                return Optional.empty();
            }
            if (!response.isSuccessful()) {
                throw refused("download", response.code());
            }
            return Optional.of(bytesOf(response.body()));
        }
    }

    @Override
    public void delete(String fileId) {
        try (Response response = call(new Request.Builder().url(fileUrl(fileId)).delete())) {
            if (!response.isSuccessful() && response.code() != HTTP_NOT_FOUND) {
                throw refused("delete", response.code());
            }
        }
    }

    /** Sends the request with a Bearer access token; a 401 drops the cached token for the next call. */
    private Response call(Request.Builder request) {
        if (!isConfigured()) {
            throw new PhotoStoreException("Drive is not configured");
        }
        try {
            Response response = http.newCall(request.header("Authorization", "Bearer " + accessToken()).build())
                    .execute();
            if (response.code() == HTTP_UNAUTHORIZED) {
                dropAccessToken();
            }
            return response;
        } catch (IOException e) {
            log.warn("regalos: could not reach Google Drive ({})", e.getClass().getSimpleName());
            throw new PhotoStoreException("Drive could not be reached");
        }
    }

    /** The cached access token, or a new one bought with the refresh token. */
    private synchronized String accessToken() throws IOException {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenExpiresAt.minus(REFRESH_MARGIN))) {
            return accessToken;
        }
        Request request = new Request.Builder()
                .url(tokenUrl)
                .post(new FormBody.Builder()
                        .add("client_id", clientId)
                        .add("client_secret", clientSecret)
                        .add("refresh_token", refreshToken)
                        .add("grant_type", "refresh_token")
                        .build())
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("regalos: Google refused the Drive refresh token (HTTP {})", response.code());
                throw new PhotoStoreException("Google refused the Drive credentials");
            }
            JsonNode answer = readJson(response.body());
            String token = answer.path("access_token").asText("");
            if (token.isEmpty()) {
                throw new PhotoStoreException("Google answered without an access token");
            }
            accessToken = token;
            accessTokenExpiresAt = now.plusSeconds(answer.path("expires_in").asLong(DEFAULT_LIFETIME.toSeconds()));
            return token;
        }
    }

    private synchronized void dropAccessToken() {
        accessToken = null;
        accessTokenExpiresAt = Instant.MIN;
    }

    private HttpUrl fileUrl(String fileId) {
        if (fileId == null || !FILE_ID.matcher(fileId).matches()) {
            throw new PhotoStoreException("Not a Drive file id");
        }
        return apiUrl.newBuilder().addPathSegments("drive/v3/files").addPathSegment(fileId).build();
    }

    private static PhotoStoreException refused(String what, int code) {
        log.warn("regalos: Google Drive refused a photo {} (HTTP {})", what, code);
        return new PhotoStoreException("Drive refused the " + what);
    }

    private static byte[] bytesOf(ResponseBody body) {
        if (body == null) {
            return new byte[0];
        }
        try (InputStream in = body.byteStream()) {
            byte[] bytes = in.readNBytes(MAX_DOWNLOAD_BYTES + 1);
            if (bytes.length > MAX_DOWNLOAD_BYTES) {
                throw new PhotoStoreException("A stored photo is larger than any photo we save");
            }
            return bytes;
        } catch (IOException e) {
            throw new PhotoStoreException("Drive cut the photo short");
        }
    }

    private JsonNode readJson(ResponseBody body) {
        if (body == null) {
            return mapper.createObjectNode();
        }
        try {
            JsonNode node = mapper.readTree(body.string());
            return node == null ? mapper.createObjectNode() : node;
        } catch (IOException e) {
            // Not JSON (an HTML error page, say): treated as an answer without details.
            return mapper.createObjectNode();
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    /** Credentials, folder and Google's endpoints (overridden in tests by a local server). */
    record Settings(String clientId, String clientSecret, String refreshToken, String folderId, String tokenUrl,
                    String apiUrl) {
    }
}
