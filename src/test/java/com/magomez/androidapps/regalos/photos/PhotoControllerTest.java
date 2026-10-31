package com.magomez.androidapps.regalos.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.magomez.androidapps.regalos.auth.RegalosCurrentUser;
import com.magomez.androidapps.regalos.config.RegalosApiExceptionHandler;
import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.lists.ListDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The photo routes as the app calls them: raw image bodies, JPEG answers and JSON errors. */
class PhotoControllerTest {

    private static final RegalosCurrentUser CRIS = new RegalosCurrentUser(1, "Cris", "hash");
    private static final String OWN = "SELECT photo_file_id FROM regalos_items WHERE id = ? AND owner_user_id = ?";
    private static final String VIEW = "LEFT JOIN regalos_claims c ON c.item_id = i.id WHERE i.id = ?";

    private RegalosFakeJdbc jdbc;
    private FakePhotoStore store;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        store = new FakePhotoStore();
        RegalosPhotoDao dao = new RegalosPhotoDao(jdbc);
        RegalosPhotoService service = new RegalosPhotoService(dao, store, new PhotoFiles(dao, store), new ListDao(jdbc),
                Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC));
        // Dates as timestamps, like the app's mapper: the fields themselves must ask for ISO-8601.
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(new PhotoController(service))
                .setControllerAdvice(new RegalosApiExceptionHandler())
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new MappingJackson2HttpMessageConverter(json))
                .build();
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request) {
        return request.requestAttr(RegalosCurrentUser.ATTRIBUTE, CRIS);
    }

    @Test
    void aJpegBodyBecomesThePhotoAndTheAnswerIsItsVersion() throws Exception {
        jdbc.rows(OWN, row("photo_file_id", null));

        mockMvc.perform(as(put("/regalos-api/v1/me/items/42/photo"))
                        .contentType(MediaType.IMAGE_JPEG).content(PhotoImagesTest.jpeg(300, 200)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoVersion").value("2026-10-31T12:00:00Z"));
    }

    /** Refused by Spring before any handler runs, so without our JSON body. */
    @Test
    void anythingButJpegOrPngIs415() throws Exception {
        mockMvc.perform(as(put("/regalos-api/v1/me/items/42/photo"))
                        .contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType());
        assertThat(store.uploadedNames).isEmpty();
    }

    @Test
    void aBodyOverTheLimitIs413() throws Exception {
        jdbc.rows(OWN, row("photo_file_id", null));

        mockMvc.perform(as(put("/regalos-api/v1/me/items/42/photo"))
                        .contentType(MediaType.IMAGE_JPEG).content(new byte[PhotoImages.MAX_BYTES + 1]))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("photo_too_large"));
    }

    @Test
    void aPhotoIsAPrivatelyCachedJpeg() throws Exception {
        store.files.put("wish-photo-id", new byte[]{(byte) 0xFF, (byte) 0xD8, 5});
        jdbc.rows(VIEW, row("owner_user_id", 2, "list_id", null, "status", "ACTIVE", "photo_file_id", "wish-photo-id",
                "marked_by_user_id", null, "giver_user_id", null));

        mockMvc.perform(as(get("/regalos-api/v1/items/42/photo")).param("v", "2026-10-31T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string("Cache-Control", "max-age=604800, private"))
                .andExpect(content().bytes(new byte[]{(byte) 0xFF, (byte) 0xD8, 5}));
    }

    @Test
    void aMissingPhotoIsAJson404EvenWhenAskingForAnImage() throws Exception {
        mockMvc.perform(as(get("/regalos-api/v1/items/42/photo")).accept(MediaType.IMAGE_JPEG, MediaType.ALL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void driveFailingIsA502WithoutDetails() throws Exception {
        store.failing = true;
        jdbc.rows(OWN, row("photo_file_id", null));

        mockMvc.perform(as(put("/regalos-api/v1/me/items/42/photo"))
                        .contentType(MediaType.IMAGE_JPEG).content(PhotoImagesTest.jpeg(30, 20)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("photo_store_error"))
                .andExpect(jsonPath("$.message").value("No se ha podido guardar o leer la foto, inténtalo más tarde"));
    }

    @Test
    void removingThePhotoIs204() throws Exception {
        jdbc.rows(OWN, row("photo_file_id", "old-photo-id"));

        mockMvc.perform(as(delete("/regalos-api/v1/me/items/42/photo")))
                .andExpect(status().isNoContent());
    }
}
