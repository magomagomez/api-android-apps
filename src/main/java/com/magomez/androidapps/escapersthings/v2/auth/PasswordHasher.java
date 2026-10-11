package com.magomez.androidapps.escapersthings.v2.auth;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Hashes passwords as {@code pbkdf2$<iterations>$<saltBase64>$<hashBase64>} with the JDK's
 * PBKDF2WithHmacSHA256, so no new dependency is needed. The iterations travel in the stored
 * value: raising {@link #ITERATIONS} later keeps the old hashes valid.
 */
@Component
public class PasswordHasher {

    public static final int ITERATIONS = 210_000;
    public static final int SALT_BYTES = 16;
    public static final int HASH_BYTES = 32;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final String SEPARATOR = "$";
    private static final int PARTS = 4;

    private final SecureRandom random = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS, HASH_BYTES);
        Base64.Encoder base64 = Base64.getEncoder();
        return String.join(SEPARATOR, PREFIX, String.valueOf(ITERATIONS),
                base64.encodeToString(salt), base64.encodeToString(hash));
    }

    /** False for a wrong password and for anything that is not a well-formed stored hash. */
    public boolean matches(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\" + SEPARATOR);
        if (parts.length != PARTS || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            Base64.Decoder base64 = Base64.getDecoder();
            byte[] salt = base64.decode(parts[2]);
            byte[] expected = base64.decode(parts[3]);
            if (iterations <= 0 || expected.length == 0) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(password, salt, iterations, expected.length));
        } catch (IllegalArgumentException e) {
            // NumberFormatException included: a damaged stored value never matches.
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations, int bytes) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bytes * Byte.SIZE);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Every JDK ships " + ALGORITHM, e);
        } finally {
            spec.clearPassword();
        }
    }
}
