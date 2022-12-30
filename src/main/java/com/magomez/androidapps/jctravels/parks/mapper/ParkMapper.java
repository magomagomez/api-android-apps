package com.magomez.androidapps.jctravels.parks.mapper;

import com.magomez.androidapps.jctravels.parks.dto.Park;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ParkMapper implements RowMapper<Park> {

    @Override
    public Park mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Park(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("queueId"),
                rs.getInt("city"));
    }
}
