package com.magomez.androidapps.jcwedding.attendant.mapper;

import com.magomez.androidapps.jcwedding.attendant.dto.Attendant;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AttendantMapper implements RowMapper<Attendant> {

    @Override
    public Attendant mapRow(ResultSet rs, int rowNum) throws SQLException {
        Attendant attendant = new Attendant();
        attendant.setId(rs.getInt("id"));
        attendant.setName(rs.getString("name"));
        attendant.setSurname(rs.getString("surname"));
        attendant.setAttendance(rs.getInt("attendance"));
        attendant.setSpecialFeatures(rs.getString("special_features"));
        return attendant;
    }
}
