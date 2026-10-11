package com.magomez.androidapps.escapersthings.v2.stats;

import com.magomez.androidapps.escapersthings.v2.rooms.Grade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class StatsDao {

    /** Rooms I played, by category: most played first. */
    private static final String PLAYED_BY_CATEGORY = """
            SELECT er.type AS category, COUNT(*) AS played
            FROM escape_room_players p
            JOIN escape_rooms er ON er.id = p.escape_room_id
            WHERE p.user_id = ?
            GROUP BY er.type
            ORDER BY played DESC, er.type
            """;
    private static final String MY_GRADES = """
            SELECT er.id, er.name, g.enigma, g.inmersion, g.horror, g.game_master, g.global
            FROM user_categoria_nota g
            JOIN escape_rooms er ON er.id = g.escape_room_id
            WHERE g.user_id = ?
            """;
    /** AVG skips the null scores. */
    private static final String GROUP_AVERAGE = """
            SELECT AVG(g.global)
            FROM user_categoria_nota g
            JOIN escape_rooms er ON er.id = g.escape_room_id
            """;

    private static final RowMapper<CategoryCount> CATEGORY = (rs, rowNum) ->
            new CategoryCount(rs.getString("category"), rs.getInt("played"));
    private static final RowMapper<MyGrade> MY_GRADE = (rs, rowNum) -> new MyGrade(rs.getInt("id"), rs.getString("name"),
            new Grade(rs.getBigDecimal("enigma"), rs.getBigDecimal("inmersion"), rs.getBigDecimal("horror"),
                    rs.getBigDecimal("game_master"), rs.getBigDecimal("global")));

    private final JdbcTemplate jdbcTemplate;

    public StatsDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<CategoryCount> playedByCategory(int userId) {
        return jdbcTemplate.query(PLAYED_BY_CATEGORY, CATEGORY, userId);
    }

    List<MyGrade> myGrades(int userId) {
        return jdbcTemplate.query(MY_GRADES, MY_GRADE, userId);
    }

    /** Null when nobody has a global score. */
    BigDecimal groupAverage() {
        return jdbcTemplate.queryForObject(GROUP_AVERAGE, BigDecimal.class);
    }
}
