package com.magomez.androidapps.jctravels.cities.mapper;

import com.magomez.androidapps.jctravels.cities.dto.City;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CityMapper implements RowMapper<City> {

    @Override
    public City mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new City(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("icon_path"),
                rs.getInt("travel"),
                rs.getBoolean("has_parks"),
                rs.getBoolean("has_monuments"),
                rs.getBoolean("has_outlets"));
    }
}
