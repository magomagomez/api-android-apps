package com.magomez.androidapps.escapersthings.v2;

import com.magomez.androidapps.escapersthings.v2.stats.Stats;
import com.magomez.androidapps.escapersthings.v2.stats.StatsDao;
import com.magomez.androidapps.escapersthings.v2.stats.StatsService;
import com.magomez.androidapps.escapersthings.v2.stats.TopEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.magomez.androidapps.escapersthings.v2.Rows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** GET /me/stats: what I played, my tops ordered like v1, my average and the group's. */
class StatsTest {

    private static final int ME = 1;
    private static final String BY_CATEGORY = "FROM escape_room_players p";
    private static final String MY_GRADES = "WHERE g.user_id = ?";
    private static final String GROUP_AVERAGE = "SELECT AVG(g.global)";

    private RecordingJdbcTemplate jdbc;
    private StatsService service;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
        service = new StatsService(new StatsDao(jdbc));
    }

    @Test
    void totalAndCategoriesCountTheRoomsIPlayedMostPlayedFirst() {
        jdbc.rows(BY_CATEGORY, row("category", "Terror", "played", 30L), row("category", "Aventura", "played", 12L));

        Stats stats = service.stats(ME);

        assertThat(stats.total()).isEqualTo(42);
        assertThat(stats.byCategory()).extracting(c -> c.category() + "=" + c.count())
                .containsExactly("Terror=30", "Aventura=12");
        assertThat(jdbc.callsWith(BY_CATEGORY).get(0).sql()).contains("ORDER BY played DESC, er.type");
        assertThat(jdbc.callsWith(BY_CATEGORY).get(0).args()).containsExactly(ME);
        assertThat(jdbc.callsWith(MY_GRADES).get(0).args()).containsExactly(ME);
    }

    @Test
    void averagesHaveTwoDecimalsAndAcceptLegacyScores() {
        jdbc.rows(MY_GRADES,
                        grade(1, "A", null, null, null, "7.66"),
                        grade(2, "B", null, null, null, "8"),
                        grade(3, "C", null, null, null, null))
                .scalar(GROUP_AVERAGE, new BigDecimal("8.123456"));

        Stats stats = service.stats(ME);

        assertThat(stats.myAverage()).isEqualByComparingTo("7.83");
        assertThat(stats.myAverage().scale()).isEqualTo(2);
        assertThat(stats.groupAverage()).isEqualByComparingTo("8.12");
        assertThat(stats.groupAverage().scale()).isEqualTo(2);
    }

    @Test
    void withoutGradesAveragesAreNullAndTopsEmpty() {
        Stats stats = service.stats(ME);

        assertThat(stats.total()).isZero();
        assertThat(stats.myAverage()).isNull();
        assertThat(stats.groupAverage()).isNull();
        assertThat(stats.tops().best()).isEmpty();
        assertThat(stats.tops().worst()).isEmpty();
    }

    @Test
    void topsHaveFiveRoomsByMyScoreSkippingEmptyOnes() {
        jdbc.rows(MY_GRADES,
                grade(1, "Uno", "2", "5", "5", "9.5"),
                grade(2, "Dos", "9", "5", "5", "8"),
                grade(3, "Tres", "0", "8", "5", "6"),
                grade(4, "Cuatro", null, "9.75", "5", "4"),
                grade(5, "Cinco", "5", "5", "10", "10"),
                grade(6, "Seis", "1", "5", "5", "7"),
                grade(7, "Siete", "3", "5", null, null));

        Stats stats = service.stats(ME);

        assertThat(names(stats.tops().best())).containsExactly("Cinco", "Uno", "Dos", "Seis", "Tres");
        assertThat(stats.tops().best().get(0).score()).isEqualByComparingTo("10");
        assertThat(names(stats.tops().worst())).containsExactly("Cuatro", "Tres", "Seis", "Dos", "Uno");
        assertThat(names(stats.tops().terror())).containsExactly("Dos", "Cinco", "Siete", "Uno", "Seis");
        assertThat(stats.tops().terror()).extracting(TopEntry::score).last()
                .satisfies(score -> assertThat(score).isEqualByComparingTo("1"));
        assertThat(names(stats.tops().immersion()).get(0)).isEqualTo("Cuatro");
        assertThat(names(stats.tops().gameMaster()).get(0)).isEqualTo("Cinco");
        assertThat(stats.tops().gameMaster()).hasSize(5).extracting(TopEntry::name).doesNotContain("Siete");
    }

    @Test
    void terrorZeroIsAScoreInTheTerrorTop() {
        jdbc.rows(MY_GRADES, grade(3, "Tres", "0", "8", "5", "6"));

        assertThat(service.stats(ME).tops().terror()).extracting(TopEntry::name).containsExactly("Tres");
    }

    @Test
    void tiesFollowV1WhereAnEmptySecondScoreSortsAsTheHighest() {
        // v1: ORDER BY global DESC, horror DESC (Postgres puts NULL first in DESC, last in ASC).
        jdbc.rows(MY_GRADES,
                grade(1, "Con terror", "7", null, null, "9"),
                grade(2, "Sin terror", null, null, null, "9"),
                grade(3, "Otra", "9", null, null, "9"));

        Stats stats = service.stats(ME);

        assertThat(names(stats.tops().best())).containsExactly("Sin terror", "Otra", "Con terror");
        assertThat(names(stats.tops().worst())).containsExactly("Con terror", "Otra", "Sin terror");
    }

    private static java.util.List<String> names(java.util.List<TopEntry> top) {
        return top.stream().map(TopEntry::name).toList();
    }

    private static Map<String, Object> grade(int roomId, String name, String terror, String immersion,
                                             String gameMaster, String global) {
        return row("id", roomId, "name", name, "enigma", null, "inmersion", number(immersion),
                "horror", number(terror), "game_master", number(gameMaster), "global", number(global));
    }

    private static BigDecimal number(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
