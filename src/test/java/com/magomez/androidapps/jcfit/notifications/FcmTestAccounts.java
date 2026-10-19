package com.magomez.androidapps.jcfit.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Throwaway Firebase service accounts for the tests: the RSA pair is generated on the fly,
 * so no real key ever lives in the repository.
 */
final class FcmTestAccounts {

    static final String PROJECT_ID = "demo-project";
    static final String CLIENT_EMAIL = "jcfit@demo-project.iam.gserviceaccount.com";
    static final KeyPair KEYS = generate();
    /** A chunk of the private key (inside one line of the PEM), to look for leaks. */
    static final String KEY_FRAGMENT = Base64.getEncoder().encodeToString(KEYS.getPrivate().getEncoded())
            .substring(70, 110);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FcmTestAccounts() {
    }

    static String pem() {
        String body = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(KEYS.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
    }

    /** The JSON Firebase downloads for a service account, pointing token_uri at tokenUri. */
    static String json(String tokenUri) {
        ObjectNode account = MAPPER.createObjectNode()
                .put("type", "service_account")
                .put("project_id", PROJECT_ID)
                .put("private_key_id", "0123456789abcdef")
                .put("private_key", pem())
                .put("client_email", CLIENT_EMAIL)
                .put("client_id", "1234567890")
                .put("auth_uri", "https://accounts.google.com/o/oauth2/auth");
        if (tokenUri != null) {
            account.put("token_uri", tokenUri);
        }
        return account.toPrettyString();
    }

    static String base64(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
