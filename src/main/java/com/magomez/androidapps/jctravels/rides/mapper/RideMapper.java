package com.magomez.androidapps.jctravels.rides.mapper;

import com.magomez.androidapps.jctravels.rides.dto.Ride;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RideMapper implements RowMapper<Ride> {

    @Override
    public Ride mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Ride(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("image_path"),
                rs.getBoolean("done"),
                rs.getInt("park"),
                rs.getBoolean("must"),
                rs.getString("code"),
                rs.getString("location_path")
        );
    }
}
