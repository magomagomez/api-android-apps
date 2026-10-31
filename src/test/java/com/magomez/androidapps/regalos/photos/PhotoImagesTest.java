package com.magomez.androidapps.regalos.photos;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static org.assertj.core.api.Assertions.assertThat;

/** R12: only real JPEG or PNG photos get in, and what is stored is a fresh, smaller JPEG without metadata. */
class PhotoImagesTest {

    private static final String SECRET = "GPS 41.3874N 2.1686E calle secreta";

    @Test
    void aPhotoComesBackAsAJpegWithoutTheMetadataOfTheOriginal() throws IOException {
        byte[] withExif = withApp1(jpeg(800, 600), "Exif\0\0" + SECRET);
        assertThat(contains(withExif, SECRET)).isTrue();

        byte[] clean = PhotoImages.clean(withExif);

        assertThat(contains(clean, SECRET)).isFalse();
        assertThat(contains(clean, "Exif")).isFalse();
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(clean));
        assertThat(stored.getWidth()).isEqualTo(800);
        assertThat(stored.getHeight()).isEqualTo(600);
    }

    @Test
    void aBigPhotoIsScaledDownToItsLongestSide() throws IOException {
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(PhotoImages.clean(jpeg(4000, 3000))));

        assertThat(stored.getWidth()).isEqualTo(PhotoImages.MAX_SIDE);
        assertThat(stored.getHeight()).isEqualTo(1200);
    }

    @Test
    void aPngWithTransparencyBecomesAJpegOnWhite() throws IOException {
        BufferedImage transparent = new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(transparent, "png", png);

        byte[] clean = PhotoImages.clean(png.toByteArray());

        assertThat(clean[0] & 0xFF).isEqualTo(0xFF);
        assertThat(clean[1] & 0xFF).isEqualTo(0xD8);
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(clean));
        assertThat(new Color(stored.getRGB(10, 10)).getRed()).isGreaterThan(240);
    }

    @Test
    void whatIsNotAJpegOrPngIsRefused() throws IOException {
        ByteArrayOutputStream gif = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "gif", gif);

        assertRegalosError(() -> PhotoImages.clean("no soy una foto".getBytes(StandardCharsets.UTF_8)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> PhotoImages.clean(gif.toByteArray()), HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> PhotoImages.clean(Arrays.copyOf(jpeg(300, 200), 40)),
                HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> PhotoImages.clean(new byte[0]), HttpStatus.BAD_REQUEST, "bad_request");
        assertRegalosError(() -> PhotoImages.clean(null), HttpStatus.BAD_REQUEST, "bad_request");
    }

    @Test
    void anUploadOverTheLimitIsRefusedBeforeDecoding() {
        assertRegalosError(() -> PhotoImages.clean(new byte[PhotoImages.MAX_BYTES + 1]),
                HttpStatus.PAYLOAD_TOO_LARGE, "photo_too_large");
    }

    @Test
    void aPngThatClaimsToBeHugeIsRefusedWithoutDecodingIt() throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", png);
        byte[] bomb = png.toByteArray();
        // IHDR width and height start at byte 16: claim 30000 x 30000.
        writeInt(bomb, 16, 30_000);
        writeInt(bomb, 20, 30_000);

        assertRegalosError(() -> PhotoImages.clean(bomb), HttpStatus.BAD_REQUEST, "bad_request");
    }

    static byte[] jpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(30, 90, 160));
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", out);
        return out.toByteArray();
    }

    /** The JPEG with an APP1 segment (where cameras put EXIF and GPS) right after its SOI marker. */
    private static byte[] withApp1(byte[] jpeg, String payload) {
        byte[] data = payload.getBytes(StandardCharsets.ISO_8859_1);
        int length = data.length + 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);
        out.write(0xFF);
        out.write(0xE1);
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.write(data, 0, data.length);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }

    private static boolean contains(byte[] haystack, String needle) {
        return new String(haystack, StandardCharsets.ISO_8859_1).contains(needle);
    }

    private static void writeInt(byte[] bytes, int at, int value) {
        bytes[at] = (byte) (value >>> 24);
        bytes[at + 1] = (byte) (value >>> 16);
        bytes[at + 2] = (byte) (value >>> 8);
        bytes[at + 3] = (byte) value;
    }
}
