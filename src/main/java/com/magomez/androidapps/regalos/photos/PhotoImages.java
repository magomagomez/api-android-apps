package com.magomez.androidapps.regalos.photos;

import com.magomez.androidapps.regalos.config.RegalosException;
import org.springframework.http.HttpStatus;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * R12: what reaches Drive is always a fresh JPEG of at most {@link #MAX_SIDE} px, decoded from a
 * real JPEG or PNG and encoded again, so no metadata of the original (GPS, camera, comments)
 * survives. The size of the picture is checked from its header before decoding it.
 */
public final class PhotoImages {

    /** Largest upload accepted, before any decoding. */
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    /** Longest side of the stored photo. */
    static final int MAX_SIDE = 1600;
    /** Larger pictures are refused before decoding: a small file can claim to be huge. */
    static final long MAX_PIXELS = 60_000_000L;

    private static final Set<String> FORMATS = Set.of("jpeg", "png");
    private static final float QUALITY = 0.85f;
    private static final String NOT_A_PHOTO = "El archivo no es una foto JPEG o PNG";

    private PhotoImages() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    public static byte[] clean(byte[] upload) {
        if (upload == null || upload.length == 0) {
            throw RegalosException.badRequest("Falta la foto");
        }
        if (upload.length > MAX_BYTES) {
            throw tooLarge();
        }
        return encode(scaled(decode(upload)));
    }

    static RegalosException tooLarge() {
        return new RegalosException(HttpStatus.PAYLOAD_TOO_LARGE, "photo_too_large",
                "La foto ocupa más de " + MAX_BYTES / (1024 * 1024) + " MB");
    }

    private static BufferedImage decode(byte[] upload) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(upload))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) {
                throw RegalosException.badRequest(NOT_A_PHOTO);
            }
            ImageReader reader = readers.next();
            try {
                if (!FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw RegalosException.badRequest(NOT_A_PHOTO);
                }
                reader.setInput(in, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_PIXELS) {
                    throw RegalosException.badRequest("La foto es demasiado grande");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw RegalosException.badRequest(NOT_A_PHOTO);
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException | IndexOutOfBoundsException e) {
            // A truncated or malformed file: the JDK decoders fail with any of these.
            throw RegalosException.badRequest(NOT_A_PHOTO);
        }
    }

    /** Fits the picture in MAX_SIDE, on a white background (JPEG has no transparency). */
    private static BufferedImage scaled(BufferedImage image) {
        double factor = Math.min(1.0, (double) MAX_SIDE / Math.max(image.getWidth(), image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * factor));
        int height = Math.max(1, (int) Math.round(image.getHeight() * factor));
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(image, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static byte[] encode(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode a JPEG in memory", e);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
