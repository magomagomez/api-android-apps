package com.magomez.androidapps.escapersthings.userstats.dao;


import com.magomez.androidapps.escapersthings.userstats.dto.Stats;
import com.magomez.androidapps.escapersthings.userstats.dto.StatsGrades;
import com.magomez.androidapps.escapersthings.userstats.mapper.GradeStatsMapper;
import com.magomez.androidapps.escapersthings.userstats.mapper.StatsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserStatsDao {

    private static final String TABLE_ESCAPEROOMS = "escape_rooms";
    private static final String COL_DONE = "done";
    private static final String COL_TYPE = "type";
    private static final String COL_JAVI_DONE = "javi_done";
    private static final String COL_CRIS_DONE = "cris_done";

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;


    public List<Stats> getStats(Integer userId) {
        String query = "select COUNT(*) AS total," + COL_TYPE;
        query = query + " from " + TABLE_ESCAPEROOMS ;
        query = query + " where " + COL_DONE +" = 1";
        if(userId == 1){
            query = query + " or "+COL_JAVI_DONE+" = 1";
        }
        if(userId == 2){
            query = query + " or "+COL_CRIS_DONE+" = 1";
        }
        query = query + " GROUP by " + COL_TYPE;
        return  jdbcTemplate.query(query , new StatsMapper());
    }

    public List<StatsGrades> getStatsGrades(Integer userId, String order ) {

        String query = "select er.name, er.id , ucn.enigma, ucn.game_master, ucn.inmersion, ucn.horror, ucn.global ";
        query = query + "from user_categoria_nota ucn ";
        query = query + "inner join  escape_rooms er on er.id = ucn.escape_room_id ";
        query = query + "where ucn.user_id = " + userId;
        query = switch (order == null ? "" : order) {
            case "W" -> query + "AND ucn.global IS NOT NULL ORDER BY ucn.global ASC, ucn.horror ASC LIMIT 5";
            case "G" -> query + "AND ucn.game_master IS NOT NULL ORDER BY ucn.game_master DESC, ucn.global DESC LIMIT 5";
            case "I" -> query + "AND ucn.inmersion IS NOT NULL ORDER BY ucn.inmersion DESC, ucn.global DESC LIMIT 5";
            case "H" -> query + "AND ucn.horror IS NOT NULL ORDER BY ucn.horror DESC, ucn.global DESC LIMIT 5";
            case "A" -> query + "AND ucn.global IS NOT NULL ORDER BY ucn.global DESC, ucn.horror DESC LIMIT 5";
            default -> query + "AND ucn.global IS NOT NULL ORDER BY ucn.global DESC, ucn.horror DESC";
        };
        return jdbcTemplate.query(query, new GradeStatsMapper());
    }
}
