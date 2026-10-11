package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.config.ApiExceptionHandler;
import com.magomez.androidapps.escapersthings.v2.config.Sha256;
import com.magomez.androidapps.escapersthings.v2.photos.PhotoDao;
import com.magomez.androidapps.escapersthings.v2.photos.PhotoService;
import com.magomez.androidapps.escapersthings.v2.photos.PhotosController;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomDao;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.magomez.androidapps.escapersthings.v2.Rows.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Photos in Postgres: real type from the first bytes, 2 MB at most, ETag and 304. */
class PhotoTest {

    private static final String URL = "/escapers-api/v2/escape-rooms/7/photos/";
    private static final String UPSERT = "INSERT INTO escape_room_photos";
    private static final String SELECT = "FROM escape_room_photos WHERE escape_room_id = ? AND kind = ?";
    private static final byte[] JPEG = bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10);
    private static final byte[] PNG = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
    private static final byte[] WEBP = "RIFF$\u0000\u0000\u0000WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] PDF = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);

    private RecordingJdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate().scalar("SELECT COUNT(*) FROM escape_rooms WHERE id = ?", 1);
        PhotoService service = new PhotoService(new PhotoDao(jdbc), new RoomService(new RoomDao(jdbc)));
        mvc = MockMvcBuilders.standaloneSetup(new PhotosController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void pngNamedFileThatIsReallyJpegIsStoredAsJpeg() throws Exception {
        mvc.perform(put(URL + "poster").contentType("image/png").content(JPEG)).andExpect(status().isNoContent());

        RecordingJdbcTemplate.Call upsert = jdbc.callsWith(UPSERT).get(0);
        assertThat(upsert.args()).containsExactly(7, "poster", "image/jpeg", JPEG, Sha256.hex(JPEG));
        assertThat(upsert.sql()).contains("ON CONFLICT (escape_room_id, kind) DO UPDATE");
    }

    @Test
    void pngAndWebpAreRecognised() throws Exception {
        mvc.perform(put(URL + "group").content(PNG)).andExpect(status().isNoContent());
        mvc.perform(put(URL + "group").content(WEBP)).andExpect(status().isNoContent());

        assertThat(jdbc.callsWith(UPSERT)).extracting(call -> call.args()[2]).containsExactly("image/png", "image/webp");
    }

    @Test
    void pdfOrEmptyBodyIsRejected() throws Exception {
        mvc.perform(put(URL + "poster").contentType("image/jpeg").content(PDF))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_photo"));
        mvc.perform(put(URL + "poster").content(new byte[0])).andExpect(status().isBadRequest());
        mvc.perform(put(URL + "poster").content("RIFF1234AVI ".getBytes(StandardCharsets.US_ASCII)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void moreThanTwoMegabytesIsRejected() throws Exception {
        byte[] big = Arrays.copyOf(JPEG, 3 * 1024 * 1024);
        byte[] limit = Arrays.copyOf(JPEG, 2 * 1024 * 1024);

        mvc.perform(put(URL + "poster").content(big))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_photo"));
        mvc.perform(put(URL + "poster").content(limit)).andExpect(status().isNoContent());
    }

    @Test
    void uploadToAnUnknownRoomOrKindIsNotFound() throws Exception {
        mvc.perform(put(URL + "cover").content(JPEG)).andExpect(status().isNotFound());
        jdbc.scalar("SELECT COUNT(*) FROM escape_rooms WHERE id = ?", 0);
        mvc.perform(put(URL + "poster").content(JPEG)).andExpect(status().isNotFound());
        assertThat(jdbc.callsWith(UPSERT)).isEmpty();
    }

    @Test
    void getServesTheRealTypeWithEtagAndPrivateCache() throws Exception {
        String etag = Sha256.hex(JPEG);
        jdbc.rows(SELECT, row("content_type", "image/jpeg", "data", JPEG, "etag", etag));

        mvc.perform(get(URL + "poster"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG))
                .andExpect(header().string("ETag", "\"" + etag + "\""))
                .andExpect(header().string("Cache-Control", "private, max-age=86400"));
        assertThat(jdbc.last().args()).containsExactly(7, "poster");
    }

    @Test
    void sameEtagIsNotModifiedAndADifferentOneGetsThePhoto() throws Exception {
        String etag = Sha256.hex(JPEG);
        jdbc.rows(SELECT, row("content_type", "image/jpeg", "data", JPEG, "etag", etag));

        mvc.perform(get(URL + "poster").header("If-None-Match", "\"" + etag + "\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", "\"" + etag + "\""))
                .andExpect(content().bytes(new byte[0]));
        mvc.perform(get(URL + "poster").header("If-None-Match", "W/\"other\", \"" + etag + "\""))
                .andExpect(status().isNotModified());
        mvc.perform(get(URL + "poster").header("If-None-Match", "\"other\""))
                .andExpect(status().isOk())
                .andExpect(content().bytes(JPEG));
    }

    @Test
    void serviceTellsWhenTheClientAlreadyHasThePhoto() {
        String etag = Sha256.hex(JPEG);
        jdbc.rows(SELECT, row("content_type", "image/jpeg", "data", JPEG, "etag", etag));
        PhotoService service = new PhotoService(new PhotoDao(jdbc), new RoomService(new RoomDao(jdbc)));

        assertThat(service.read(7, "poster", "\"" + etag + "\"").notModified()).isTrue();
        assertThat(service.read(7, "poster", "W/\"" + etag + "\"").notModified()).isTrue();
        assertThat(service.read(7, "poster", "*").notModified()).isTrue();
        assertThat(service.read(7, "poster", etag).notModified()).isFalse();
        assertThat(service.read(7, "poster", "\"other\"").notModified()).isFalse();
        assertThat(service.read(7, "poster", null).notModified()).isFalse();
    }

    @Test
    void missingPhotoIsNotFound() throws Exception {
        mvc.perform(get(URL + "group").accept("image/*"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }
}
