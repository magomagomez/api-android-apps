package com.magomez.androidapps.jctravels.outlets.mapper;

import com.magomez.androidapps.jctravels.outlets.dto.Outlet;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OutletMapper implements RowMapper<Outlet> {

    @Override
    public Outlet mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Outlet(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("icon_path"),
                rs.getInt("city"));
    }
}
