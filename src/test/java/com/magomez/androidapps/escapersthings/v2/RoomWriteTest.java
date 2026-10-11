package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.rooms.RoomDao;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomService;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomSummary;
import com.magomez.androidapps.escapersthings.v2.rooms.SaveRoomRequest;
import com.magomez.androidapps.escapersthings.v2.config.ApiException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.magomez.androidapps.escapersthings.v2.ApiAssertions.assertApiError;
import static com.magomez.androidapps.escapersthings.v2.RoomReadTest.room;
import static org.assertj.core.api.Assertions.assertThat;

/** Adding, editing and deleting rooms: validation first, values always as parameters. */
class RoomWriteTest {

    private static final String ROOM_BY_ID = "FROM escape_rooms WHERE id = ?";

    private RecordingJdbcTemplate jdbc;
    private RoomService service;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
        service = new RoomService(new RoomDao(jdbc));
    }

    @Test
    void createTrimsAndUsesTheSequenceForTheId() {
        jdbc.scalar("INSERT INTO escape_rooms", 230).rows(ROOM_BY_ID, room(230, "Kemet", "Histórico", "Madrid"));

        RoomSummary created = service.create(new SaveRoomRequest("  Kemet ", "Histórico", " Madrid  "), 1);

        RecordingJdbcTemplate.Call insert = jdbc.calls.get(0);
        assertThat(insert.sql()).isEqualTo("INSERT INTO escape_rooms (name, type, ubication) VALUES (?, ?, ?) RETURNING id");
        assertThat(insert.args()).containsExactly("Kemet", "Histórico", "Madrid");
        assertThat(jdbc.callsWith("MAX(")).isEmpty();
        assertThat(created.id()).isEqualTo(230);
        assertThat(created.played()).isFalse();
    }

    @Test
    void blankCityIsStoredAsNull() {
        jdbc.scalar("INSERT INTO escape_rooms", 230).rows(ROOM_BY_ID, room(230, "Kemet", "Histórico", null));

        service.create(new SaveRoomRequest("Kemet", "Histórico", "   "), 1);

        assertThat(jdbc.calls.get(0).args()).containsExactly("Kemet", "Histórico", null);
    }

    @Test
    void categoryOutsideTheClosedListIsRejectedWithTheList() {
        ApiException error = assertInvalid(() -> service.create(new SaveRoomRequest("Kemet", "Cinefilo", null), 1));

        assertThat(error.getMessage()).contains("Cinéfilo").contains("Battle Royale");
        assertInvalid(() -> service.create(new SaveRoomRequest("Kemet", null, null), 1));
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void everyCategoryOfTheListIsAccepted() {
        jdbc.scalar("INSERT INTO escape_rooms", 1).rows(ROOM_BY_ID, room(1, "X", "Terror", null));
        for (String category : new String[]{"Aventura", "Terror", "Cinéfilo", "Tensión", "Histórico", "Hall Escape",
                "Battle Royale"}) {
            service.create(new SaveRoomRequest("X", category, null), 1);
        }
        assertThat(jdbc.callsWith("INSERT")).hasSize(7);
    }

    @Test
    void nameIsRequiredAndShort() {
        assertInvalid(() -> service.create(new SaveRoomRequest("  ", "Terror", null), 1));
        assertInvalid(() -> service.create(new SaveRoomRequest(null, "Terror", null), 1));
        assertInvalid(() -> service.create(new SaveRoomRequest("x".repeat(121), "Terror", null), 1));
        assertInvalid(() -> service.create(null, 1));
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void cityIsAtMostEightyCharacters() {
        assertInvalid(() -> service.create(new SaveRoomRequest("Kemet", "Terror", "x".repeat(81)), 1));
    }

    @Test
    void updateBindsTheIdLastAndUnknownRoomIsNotFound() {
        jdbc.rows(ROOM_BY_ID, room(7, "Profana", "Terror", "Madrid"));

        service.update(7, new SaveRoomRequest("Profana", "Terror", "Madrid"), 1);

        assertThat(jdbc.calls.get(0).sql()).isEqualTo("UPDATE escape_rooms SET name = ?, type = ?, ubication = ? WHERE id = ?");
        assertThat(jdbc.calls.get(0).args()).containsExactly("Profana", "Terror", "Madrid", 7);

        jdbc.updateCount("UPDATE escape_rooms", 0);
        assertApiError(() -> service.update(9, new SaveRoomRequest("X", "Terror", null), 1), HttpStatus.NOT_FOUND,
                "not_found");
    }

    @Test
    void deleteBindsTheIdAndUnknownRoomIsNotFound() {
        service.delete(7);
        assertThat(jdbc.last().sql()).isEqualTo("DELETE FROM escape_rooms WHERE id = ?");
        assertThat(jdbc.last().args()).containsExactly(7);

        jdbc.updateCount("DELETE FROM escape_rooms", 0);
        assertApiError(() -> service.delete(9), HttpStatus.NOT_FOUND, "not_found");
    }

    private static ApiException assertInvalid(ThrowingCallable call) {
        return assertApiError(call, HttpStatus.BAD_REQUEST, "invalid_room");
    }
}
