package com.magomez.androidapps.jctravels.routes.mapper;

import com.magomez.androidapps.jctravels.routes.dto.Route;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RouteMapper implements RowMapper<Route> {

    @Override
    public Route mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Route(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("city"),
                rs.getString("day"),
                rs.getString("month"),
                rs.getInt("sort")
        );
    }
}
