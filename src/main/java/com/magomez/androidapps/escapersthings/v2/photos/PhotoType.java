package com.magomez.androidapps.escapersthings.v2.photos;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * The real type of an image, read from its first bytes; the file name and the Content-Type the
 * client sends are ignored (many old posters are JPEGs named .png).
 */
final class PhotoType {

    static final String JPEG = "image/jpeg";
    static final String PNG = "image/png";
    static final String WEBP = "image/webp";

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MAGIC = "WEBP".getBytes(StandardCharsets.US_ASCII);
    private static final int WEBP_MAGIC_OFFSET = 8;

    private PhotoType() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    static Optional<String> detect(byte[] data) {
        if (startsWith(data, 0, JPEG_MAGIC)) {
            return Optional.of(JPEG);
        }
        if (startsWith(data, 0, PNG_MAGIC)) {
            return Optional.of(PNG);
        }
        if (startsWith(data, 0, RIFF) && startsWith(data, WEBP_MAGIC_OFFSET, WEBP_MAGIC)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, byte[] magic) {
        return data.length >= offset + magic.length
                && Arrays.equals(data, offset, offset + magic.length, magic, 0, magic.length);
    }
}
