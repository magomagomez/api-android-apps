package com.magomez.androidapps.regalos.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 in lowercase hex: the stored form of regalos session tokens. */
public final class RegalosSha256 {

    private static final String ALGORITHM = "SHA-256";

    private RegalosSha256() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    public static String hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(ALGORITHM).digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every JDK ships SHA-256", e);
        }
    }

    public static String hex(String text) {
        return hex(text.getBytes(StandardCharsets.UTF_8));
    }
}
