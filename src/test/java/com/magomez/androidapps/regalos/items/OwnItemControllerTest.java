package com.magomez.androidapps.regalos.items;

import com.magomez.androidapps.regalos.auth.RegalosCurrentUser;
import com.magomez.androidapps.regalos.config.RegalosApiExceptionHandler;
import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.magomez.androidapps.regalos.items.ItemServiceTest.item;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The JSON of my letter, as the app reads it. */
class OwnItemControllerTest {

    private static final RegalosCurrentUser CRIS = new RegalosCurrentUser(2, "Cris", "hash");
    private static final String MY_ITEMS = "FROM regalos_items WHERE owner_user_id = ? AND status = ?";
    private static final String MY_ITEM = "FROM regalos_items WHERE owner_user_id = ? AND id = ?";

    private RegalosFakeJdbc jdbc;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        // Dates as timestamps, like the app's mapper: the fields themselves must ask for ISO-8601.
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(new OwnItemController(new ItemService(new ItemDao(jdbc))))
                .setControllerAdvice(new RegalosApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .build();
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request) {
        return request.requestAttr(RegalosCurrentUser.ATTRIBUTE, CRIS);
    }

    /** R1 in the contract's own words: an OwnItem has no claim field at all. */
    @Test
    void myWishesHaveNoClaimField() throws Exception {
        jdbc.rows(MY_ITEMS, item(42, "Kit de bonsái", "ACTIVE"));

        mockMvc.perform(as(get("/regalos-api/v1/me/items")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].title").value("Kit de bonsái"))
                .andExpect(jsonPath("$[0].priceCents").value(3450))
                .andExpect(jsonPath("$[0].wantedALot").value(false))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-24T10:00:00Z"))
                .andExpect(jsonPath("$[0].claim").doesNotExist());
    }

    @Test
    void theStatusFilterIsReadFromTheQuery() throws Exception {
        mockMvc.perform(as(get("/regalos-api/v1/me/items").param("status", "RECEIVED")))
                .andExpect(status().isOk());
        mockMvc.perform(as(get("/regalos-api/v1/me/items").param("status", "PERDIDO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void createIs201() throws Exception {
        jdbc.scalar("INSERT INTO regalos_items", 42L).rows(MY_ITEM, item(42, "Sony", "ACTIVE"));

        mockMvc.perform(as(post("/regalos-api/v1/me/items")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Sony\",\"url\":\"https://www.amazon.es/dp/1\",\"wantedALot\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42));
    }

    /** R5: claimed or not, the owner gets the same 204. */
    @Test
    void deleteIsAlways204ForMyWish() throws Exception {
        mockMvc.perform(as(delete("/regalos-api/v1/me/items/42"))).andExpect(status().isNoContent());

        jdbc.updateCount("UPDATE regalos_items SET status = 'DELETED'", 0);
        mockMvc.perform(as(delete("/regalos-api/v1/me/items/42"))).andExpect(status().isNoContent());
    }

    @Test
    void receivedRoundTrip() throws Exception {
        jdbc.rows(MY_ITEM, item(42, "Sony", "RECEIVED"));
        mockMvc.perform(as(post("/regalos-api/v1/me/items/42/received")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        jdbc.rows(MY_ITEM, item(42, "Sony", "ACTIVE"));
        mockMvc.perform(as(delete("/regalos-api/v1/me/items/42/received")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
