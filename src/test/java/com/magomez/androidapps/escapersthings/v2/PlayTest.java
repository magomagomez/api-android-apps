package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.auth.UserDao;
import com.magomez.androidapps.escapersthings.v2.rooms.Grade;
import com.magomez.androidapps.escapersthings.v2.rooms.PlayDao;
import com.magomez.androidapps.escapersthings.v2.rooms.PlayService;
import com.magomez.androidapps.escapersthings.v2.rooms.PlayersRequest;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomDao;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.magomez.androidapps.escapersthings.v2.ApiAssertions.assertApiError;
import static com.magomez.androidapps.escapersthings.v2.RoomReadTest.room;
import static com.magomez.androidapps.escapersthings.v2.Rows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** "Hemos jugado" and "Puntuar": players of a room and my own grade. */
class PlayTest {

    private static final int ROOM = 7;
    private static final int ME = 1;
    private static final String ROOM_EXISTS = "SELECT COUNT(*) FROM escape_rooms WHERE id = ?";
    private static final String IS_PLAYER = "FROM escape_room_players WHERE escape_room_id = ? AND user_id = ?";
    private static final String UPSERT = "INSERT INTO user_categoria_nota";

    private RecordingJdbcTemplate jdbc;
    private PlayService service;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate()
                .scalar(ROOM_EXISTS, 1)
                .scalar(IS_PLAYER, 1)
                .rows("FROM escape_rooms WHERE id = ?", room(ROOM, "Profana", "Terror", "Madrid"))
                .rows("SELECT id, name FROM users", row("id", 1, "name", "Magomez"), row("id", 2, "name", "Cris"),
                        row("id", 3, "name", "Alex"));
        service = new PlayService(new PlayDao(jdbc), new UserDao(jdbc), new RoomService(new RoomDao(jdbc)));
    }

    @Test
    void playersReplaceTheListWithTheDateAndKeepTheGrades() {
        LocalDate day = LocalDate.of(2026, 10, 10);

        service.replacePlayers(ROOM, new PlayersRequest(List.of(3, 1, 3), day), ME);

        assertThat(jdbc.callsWith("DELETE FROM escape_room_players WHERE escape_room_id = ?").get(0).args())
                .containsExactly(ROOM);
        assertThat(jdbc.callsWith("INSERT INTO escape_room_players (escape_room_id, user_id, played_on) VALUES (?, ?, ?)"))
                .extracting(call -> Arrays.asList(call.args()))
                .containsExactly(List.of(ROOM, 1, day), List.of(ROOM, 3, day));
        assertThat(jdbc.callsWith("user_categoria_nota").stream().filter(c -> !c.sql().startsWith("SELECT")))
                .isEmpty();
    }

    @Test
    void emptyListLeavesTheRoomUnplayed() {
        service.replacePlayers(ROOM, new PlayersRequest(List.of(), null), ME);

        assertThat(jdbc.callsWith("DELETE FROM escape_room_players")).hasSize(1);
        assertThat(jdbc.callsWith("INSERT")).isEmpty();
    }

    @Test
    void unknownPlayerOrMissingListIsRejected() {
        assertApiError(() -> service.replacePlayers(ROOM, new PlayersRequest(List.of(1, 6), null), ME),
                HttpStatus.BAD_REQUEST, "invalid_players");
        assertApiError(() -> service.replacePlayers(ROOM, new PlayersRequest(null, null), ME),
                HttpStatus.BAD_REQUEST, "invalid_players");
        assertThat(jdbc.callsWith("DELETE")).isEmpty();
    }

    @Test
    void playersOfAnUnknownRoomIsNotFound() {
        jdbc.scalar(ROOM_EXISTS, 0);

        assertApiError(() -> service.replacePlayers(404, new PlayersRequest(List.of(1), null), ME),
                HttpStatus.NOT_FOUND, "not_found");
        assertThat(jdbc.callsWith("DELETE")).isEmpty();
    }

    @Test
    void gradingARoomIDidNotPlayIsAConflict() {
        jdbc.scalar(IS_PLAYER, 0);

        assertApiError(() -> service.saveMyGrade(ROOM, grade("8", "8", "8", "8", "8"), ME), HttpStatus.CONFLICT,
                "not_a_player");
        assertThat(jdbc.callsWith(UPSERT)).isEmpty();
        assertThat(jdbc.callsWith(IS_PLAYER).get(0).args()).containsExactly(ROOM, ME);
    }

    @Test
    void scoresOffTheQuarterStepOrOutOfRangeAreRejected() {
        assertApiError(() -> service.saveMyGrade(ROOM, grade("7.3", null, null, null, null), ME),
                HttpStatus.BAD_REQUEST, "invalid_grade");
        assertApiError(() -> service.saveMyGrade(ROOM, grade(null, null, null, null, "10.25"), ME),
                HttpStatus.BAD_REQUEST, "invalid_grade");
        assertApiError(() -> service.saveMyGrade(ROOM, grade(null, null, "-0.25", null, null), ME),
                HttpStatus.BAD_REQUEST, "invalid_grade");
        assertApiError(() -> service.saveMyGrade(ROOM, grade(null, "7.66", null, null, null), ME),
                HttpStatus.BAD_REQUEST, "invalid_grade");
        assertApiError(() -> service.saveMyGrade(ROOM, null, ME), HttpStatus.BAD_REQUEST, "invalid_grade");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void quarterPointsAndTerrorZeroAreSavedAsParameters() {
        service.saveMyGrade(ROOM, grade("7.25", "10", "0", "9.50", null), ME);

        RecordingJdbcTemplate.Call upsert = jdbc.callsWith(UPSERT).get(0);
        assertThat(upsert.sql()).contains("(user_id, escape_room_id, enigma, inmersion, horror, game_master, global)")
                .contains("ON CONFLICT (user_id, escape_room_id) DO UPDATE");
        assertThat(upsert.args()).containsExactly(ME, ROOM, new BigDecimal("7.25"), new BigDecimal("10"),
                new BigDecimal("0"), new BigDecimal("9.50"), null);
    }

    @Test
    void everyScoreEmptyRemovesMyGrade() {
        service.saveMyGrade(ROOM, grade(null, null, null, null, null), ME);

        assertThat(jdbc.callsWith(UPSERT)).isEmpty();
        RecordingJdbcTemplate.Call delete = jdbc.callsWith("DELETE FROM user_categoria_nota").get(0);
        assertThat(delete.sql()).isEqualTo("DELETE FROM user_categoria_nota WHERE user_id = ? AND escape_room_id = ?");
        assertThat(delete.args()).containsExactly(ME, ROOM);
    }

    @Test
    void gradingAnUnknownRoomIsNotFound() {
        jdbc.scalar(ROOM_EXISTS, 0);

        assertApiError(() -> service.saveMyGrade(404, grade("8", null, null, null, null), ME), HttpStatus.NOT_FOUND,
                "not_found");
    }

    private static Grade grade(String enigma, String immersion, String terror, String gameMaster, String global) {
        return new Grade(number(enigma), number(immersion), number(terror), number(gameMaster), number(global));
    }

    private static BigDecimal number(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
