package com.magomez.androidapps.jctravels.monuments.mapper;

import com.magomez.androidapps.jctravels.monuments.dto.Monument;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MonumentMapper implements RowMapper<Monument> {

    @Override
    public Monument mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Monument(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("image_path"),
                rs.getBoolean("done"),
                rs.getInt("city"),
                rs.getString("schedule"),
                rs.getString("price"),
                rs.getInt("route"),
                rs.getInt("priority"));
    }
}
