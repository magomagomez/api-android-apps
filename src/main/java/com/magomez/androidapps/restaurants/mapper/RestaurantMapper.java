package com.magomez.androidapps.restaurants.mapper;

import com.magomez.androidapps.restaurants.dto.Restaurant;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class RestaurantMapper implements RowMapper<Restaurant> {

    @Override
    public Restaurant mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Restaurant(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("cuisine"),
                rs.getObject("price_level", Integer.class),
                rs.getString("website"),
                rs.getString("location"),
                rs.getString("drive_folder_url"),
                rs.getString("why_go"),
                rs.getObject("visited_on", LocalDate.class),
                rs.getBigDecimal("rating"),
                rs.getString("comment"));
    }
}
