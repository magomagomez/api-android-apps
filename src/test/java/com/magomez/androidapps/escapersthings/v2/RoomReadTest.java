package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.rooms.Averages;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomDao;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomDetail;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomService;
import com.magomez.androidapps.escapersthings.v2.rooms.RoomSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.magomez.androidapps.escapersthings.v2.ApiAssertions.assertApiError;
import static com.magomez.androidapps.escapersthings.v2.Rows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** Reading rooms: averages that ignore empty scores, players, my grade and photo etags. */
class RoomReadTest {

    private static final String ROOMS = "FROM escape_rooms ORDER BY name, id";
    private static final String ROOM_BY_ID = "FROM escape_rooms WHERE id = ?";
    private static final String PLAYERS = "FROM escape_room_players";
    private static final String GRADES = "FROM user_categoria_nota";
    private static final String PHOTOS = "FROM escape_room_photos";
    private static final int MAGOMEZ = 1;
    private static final int SARA = 4;

    private RecordingJdbcTemplate jdbc;
    private RoomService service;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
        service = new RoomService(new RoomDao(jdbc));
    }

    @Test
    void profanaAveragesMatchTheGroupNotes() {
        jdbc.rows(ROOMS, room(7, "Profana", "Terror", "Madrid"))
                .rows(GRADES,
                        grade(7, 1, "Magomez", "9.5", "10", "9", "9.5", "10"),
                        grade(7, 2, "Cris", "9.5", "10", "9", "9.5", "10"),
                        grade(7, 3, "Alex", "9", "10", "8.5", "9.5", "10"),
                        grade(7, 5, "Uri", "9.5", "10", "9", "9", "9.5"));

        Averages averages = service.list(MAGOMEZ).get(0).averages();

        assertThat(averages.enigma()).isEqualByComparingTo("9.38");
        assertThat(averages.immersion()).isEqualByComparingTo("10");
        assertThat(averages.terror()).isEqualByComparingTo("8.88");
        assertThat(averages.gameMaster()).isEqualByComparingTo("9.38");
        assertThat(averages.global()).isEqualByComparingTo("9.88");
        assertThat(averages.global().scale()).isEqualTo(2);
    }

    @Test
    void legacyScoresOffTheQuarterStepAreReadAndAveraged() {
        jdbc.rows(ROOMS, room(7, "Inmortum2", "Terror", "Gironella"))
                .rows(GRADES,
                        grade(7, 1, "Magomez", null, null, null, null, "7.66"),
                        grade(7, 2, "Cris", null, null, null, null, "7.8"),
                        grade(7, 3, "Alex", null, null, null, null, "8.2"));

        RoomSummary room = service.list(MAGOMEZ).get(0);

        assertThat(room.myGrade().global()).isEqualByComparingTo("7.66");
        assertThat(room.averages().global()).isEqualByComparingTo("7.89");
    }

    @Test
    void averagesIgnoreEmptyScoresButTerrorZeroCounts() {
        jdbc.rows(ROOMS, room(9, "Olimpo", "Aventura", "Barcelona"))
                .rows(GRADES,
                        grade(9, 1, "Magomez", "8", null, "0", null, "8"),
                        grade(9, 2, "Cris", null, null, "3", null, "9"));

        Averages averages = service.list(MAGOMEZ).get(0).averages();

        assertThat(averages.enigma()).isEqualByComparingTo("8");
        assertThat(averages.terror()).isEqualByComparingTo("1.5");
        assertThat(averages.immersion()).isNull();
        assertThat(averages.gameMaster()).isNull();
    }

    @Test
    void playedAndMyGradeAreThoseOfTheTokenUser() {
        jdbc.rows(ROOMS, room(9, "Olimpo", "Aventura", null))
                .rows(PLAYERS,
                        player(9, 2, null),
                        player(9, MAGOMEZ, LocalDate.of(2026, 5, 3)))
                .rows(GRADES, grade(9, MAGOMEZ, "Magomez", "8", "7.25", "0", "9", "8.5"));

        RoomSummary mine = service.list(MAGOMEZ).get(0);
        RoomSummary saras = service.list(SARA).get(0);

        assertThat(mine.playerIds()).containsExactly(1, 2);
        assertThat(mine.played()).isTrue();
        assertThat(mine.playedOn()).isEqualTo(LocalDate.of(2026, 5, 3));
        assertThat(mine.myGrade().immersion()).isEqualByComparingTo("7.25");
        assertThat(mine.gradeCount()).isEqualTo(1);
        assertThat(saras.played()).isFalse();
        assertThat(saras.playedOn()).isNull();
        assertThat(saras.myGrade()).isNull();
    }

    @Test
    void gradeRowWithEveryScoreEmptyDoesNotCount() {
        jdbc.rows(ROOMS, room(9, "Olimpo", "Aventura", null))
                .rows(GRADES, grade(9, MAGOMEZ, "Magomez", null, null, null, null, null));

        RoomSummary room = service.list(MAGOMEZ).get(0);

        assertThat(room.gradeCount()).isZero();
        assertThat(room.myGrade()).isNull();
        assertThat(room.averages().global()).isNull();
    }

    @Test
    void photosCarryTheirEtagOrAreNull() {
        jdbc.rows(ROOMS, room(9, "Olimpo", "Aventura", null), room(10, "Kemet", "Histórico", null))
                .rows(PHOTOS, row("escape_room_id", 9, "kind", "poster", "etag", "ab12"));

        List<RoomSummary> rooms = service.list(MAGOMEZ);

        assertThat(rooms.get(0).poster().etag()).isEqualTo("ab12");
        assertThat(rooms.get(0).groupPhoto()).isNull();
        assertThat(rooms.get(1).poster()).isNull();
    }

    @Test
    void listMapsEachRowToItsRoomInFourQueries() {
        jdbc.rows(ROOMS, room(9, "Olimpo", "Aventura", null), room(10, "Kemet", "Histórico", "Madrid"))
                .rows(GRADES, grade(10, 2, "Cris", null, null, null, null, "6"));

        List<RoomSummary> rooms = service.list(MAGOMEZ);

        assertThat(rooms).extracting(RoomSummary::name).containsExactly("Olimpo", "Kemet");
        assertThat(rooms.get(0).gradeCount()).isZero();
        assertThat(rooms.get(1).averages().global()).isEqualByComparingTo("6");
        assertThat(rooms.get(1).category()).isEqualTo("Histórico");
        assertThat(rooms.get(1).city()).isEqualTo("Madrid");
        assertThat(jdbc.calls).hasSize(4);
    }

    @Test
    void detailListsEveryonesGradesByUserAndBindsTheId() {
        jdbc.rows(ROOM_BY_ID, room(7, "Profana", "Terror", "Madrid"))
                .rows(GRADES,
                        grade(7, 2, "Cris", "9", null, null, null, "9"),
                        grade(7, 1, "Magomez", "8", null, null, null, "10"));

        RoomDetail detail = service.get(7, MAGOMEZ);

        assertThat(detail.grades()).extracting(g -> g.user().name()).containsExactly("Magomez", "Cris");
        assertThat(detail.summary().myGrade().global()).isEqualByComparingTo("10");
        assertThat(jdbc.calls).allSatisfy(call -> assertThat(call.args()).containsExactly(7));
        assertThat(jdbc.callsWith(GRADES).get(0).sql()).endsWith("WHERE g.escape_room_id = ? ORDER BY g.user_id");
    }

    @Test
    void unknownRoomIsNotFound() {
        assertApiError(() -> service.get(404, MAGOMEZ), HttpStatus.NOT_FOUND, "not_found");
    }

    static Map<String, Object> room(int id, String name, String type, String city) {
        return row("id", id, "name", name, "type", type, "ubication", city);
    }

    static Map<String, Object> player(int roomId, int userId, LocalDate playedOn) {
        return row("escape_room_id", roomId, "user_id", userId, "played_on", playedOn);
    }

    static Map<String, Object> grade(int roomId, int userId, String userName, String enigma, String immersion,
                                     String terror, String gameMaster, String global) {
        return row("escape_room_id", roomId, "user_id", userId, "user_name", userName,
                "enigma", number(enigma), "inmersion", number(immersion), "horror", number(terror),
                "game_master", number(gameMaster), "global", number(global));
    }

    private static BigDecimal number(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
