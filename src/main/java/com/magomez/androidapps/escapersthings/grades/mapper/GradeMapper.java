package com.magomez.androidapps.escapersthings.grades.mapper;

import com.magomez.androidapps.escapersthings.grades.dto.Grade;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GradeMapper implements RowMapper<Grade> {

    @Override
    public Grade mapRow(ResultSet rs, int rowNum) throws SQLException{
        Grade grade = new Grade();
        grade.setName(rs.getString("name"));
        grade.setEnigma(rs.getBigDecimal("enigma"));
        grade.setGameMaster(rs.getBigDecimal("game_master"));
        grade.setGlobal(rs.getBigDecimal("global"));
        grade.setHorror(rs.getBigDecimal("horror"));
        grade.setInmersion(rs.getBigDecimal("inmersion"));
        return grade;
    }
}
