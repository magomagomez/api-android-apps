package com.magomez.androidapps.escapersthings.userstats.mapper;

import com.magomez.androidapps.escapersthings.userstats.dto.Stats;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StatsMapper implements RowMapper<Stats> {

    @Override
    public Stats mapRow(ResultSet rs, int rowNum) throws SQLException{
        return new Stats(
                rs.getString("total"),
                rs.getString("type"));
    }
}
