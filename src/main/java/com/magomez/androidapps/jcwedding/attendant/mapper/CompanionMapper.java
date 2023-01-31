package com.magomez.androidapps.jcwedding.attendant.mapper;

import com.magomez.androidapps.jcwedding.attendant.dto.Companion;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CompanionMapper implements RowMapper<Companion> {

    @Override
    public Companion mapRow(ResultSet rs, int rowNum) throws SQLException {
        Companion companion = new Companion();
        companion.setId(rs.getInt("id"));
        companion.setName(rs.getString("name"));
        companion.setAttendance(rs.getInt("attendance"));
        return companion;
    }
}
