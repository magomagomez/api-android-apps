package com.magomez.androidapps.escapersthings.userstats.mapper;

import com.magomez.androidapps.escapersthings.userstats.dto.StatsGrades;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GradeStatsMapper implements RowMapper<StatsGrades> {

    @Override
    public StatsGrades mapRow(ResultSet rs, int rowNum) throws SQLException{
        return new StatsGrades(
                rs.getString("name"),
                rs.getInt("id"),
                rs.getDouble("enigma"),
                rs.getDouble("game_master"),
                rs.getDouble("inmersion"),
                rs.getDouble("horror"),
                rs.getDouble("global"));
    }
}
