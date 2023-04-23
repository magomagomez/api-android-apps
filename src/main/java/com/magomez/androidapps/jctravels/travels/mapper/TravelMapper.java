package com.magomez.androidapps.jctravels.travels.mapper;

import com.magomez.androidapps.jctravels.travels.dto.Travel;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TravelMapper implements RowMapper<Travel> {

    @Override
    public Travel mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Travel(
                rs.getInt("id"),
                rs.getString("name"));
    }
}
