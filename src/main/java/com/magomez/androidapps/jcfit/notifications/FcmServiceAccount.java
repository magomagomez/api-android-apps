package com.magomez.androidapps.jcfit.notifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

/**
 * The parts of a Firebase service account JSON that FCM HTTP v1 needs. Errors never carry the
 * value they were given (nor a cause that might): it holds a private key.
 */
record FcmServiceAccount(String projectId, String clientEmail, PrivateKey privateKey, String tokenUri) {

    static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_FOOTER = "-----END PRIVATE KEY-----";
    private static final String WHITESPACE = "\\s+";
    private static final String JSON_START = "{";

    /**
     * Empty when the value is blank (not configured); IllegalArgumentException when it is not
     * a usable service account, given as is or in base64.
     */
    static Optional<FcmServiceAccount> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.strip();
        String json = trimmed.startsWith(JSON_START) ? trimmed : decodeBase64(trimmed);
        JsonNode account;
        try {
            account = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("it is not JSON");
        }
        if (account == null || !account.isObject()) {
            throw new IllegalArgumentException("it is not a JSON object");
        }
        String tokenUri = text(account, "token_uri");
        return Optional.of(new FcmServiceAccount(required(account, "project_id"), required(account, "client_email"),
                privateKey(required(account, "private_key")), tokenUri == null ? DEFAULT_TOKEN_URI : tokenUri));
    }

    @Override
    public String toString() {
        return "FcmServiceAccount[projectId=" + projectId + ", clientEmail=" + clientEmail + ", tokenUri=" + tokenUri
                + "]";
    }

    private static String decodeBase64(String value) {
        try {
            return new String(Base64.getDecoder().decode(value.replaceAll(WHITESPACE, "")), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("it is neither JSON nor base64");
        }
    }

    private static PrivateKey privateKey(String pem) {
        String body = pem.replace(PEM_HEADER, "").replace(PEM_FOOTER, "").replaceAll(WHITESPACE, "");
        try {
            byte[] der = Base64.getDecoder().decode(body);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IllegalArgumentException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("private_key is not a PKCS#8 RSA key in PEM");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every JDK ships RSA");
        }
    }

    private static String required(JsonNode account, String field) {
        String value = text(account, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is missing");
        }
        return value;
    }

    private static String text(JsonNode account, String field) {
        JsonNode value = account.get(field);
        return value == null || !value.isTextual() || value.asText().isBlank() ? null : value.asText();
    }
}
