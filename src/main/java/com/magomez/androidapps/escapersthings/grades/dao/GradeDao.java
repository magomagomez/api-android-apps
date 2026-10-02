package com.magomez.androidapps.escapersthings.grades.dao;


import com.magomez.androidapps.escapersthings.grades.mapper.GradeMapper;
import com.magomez.androidapps.escapersthings.grades.dto.Grade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GradeDao {

    private static final String TABLE_GRADE = "user_categoria_nota";
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_NAME = "name";
    private static final String COL_ENIGMA = "enigma";
    private static final String COL_ID = "id";
    private static final String COL_ESCAPE_ID = "escape_room_id";
    private static final String COL_GM = "game_master";
    private static final String COL_INM = "inmersion";
    private static final String COL_HORROR = "horror";
    private static final String COL_GLOBAL = "global";

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    public List<Grade> getGrades(Integer escapeId) {
        String query = "select u."+COL_NAME+", cn."+COL_ENIGMA+", cn."+COL_GM+", cn."+COL_INM+", cn."+COL_HORROR+", cn."+ COL_GLOBAL
                + " from " + TABLE_GRADE + " cn "
                + " inner join " + TABLE_USERS+ " u on u."+COL_ID+" = cn."+COL_USER_ID
                + " where cn." + COL_ESCAPE_ID + " = ?"
                + " order by cn."+COL_USER_ID;
        return jdbcTemplate.query(query, new GradeMapper(), escapeId);
    }

    public Grade getGradeByUserId(Integer escapeId, Integer userId) {
        String query = "select u."+COL_NAME+", cn."+COL_ENIGMA+", cn."+COL_GM+", cn."+COL_INM+", cn."+COL_HORROR+", cn."+ COL_GLOBAL
                + " from " + TABLE_GRADE + " cn "
                + " inner join " + TABLE_USERS+ " u on u."+COL_ID+" = cn."+COL_USER_ID
                + " where cn." + COL_ESCAPE_ID + " = ? and cn." + COL_USER_ID + " = ?";
        try {
            return jdbcTemplate.queryForObject(query, new GradeMapper(), escapeId, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void updateGrade(Integer escapeId, Integer userId , Grade grade) {
        String query = "UPDATE " + TABLE_GRADE + " set " +
                Stream.of(COL_ENIGMA,COL_GM,COL_INM,COL_HORROR,COL_GLOBAL)
                        .map(s-> s+" = :" + s)
                        .collect(Collectors.joining(",")) +
                " WHERE " +
                COL_USER_ID + " = :" + COL_USER_ID + " and " + COL_ESCAPE_ID + " = :" + COL_ESCAPE_ID;
        Map<String, Object> parameters = getParametersUpdate(grade);
        parameters.put(COL_USER_ID, userId);
        parameters.put(COL_ESCAPE_ID, escapeId);
        namedParameterJdbcTemplate.update(query, parameters);
    }

    public void insertGrade(Integer escapeId, Integer userId , Grade grade) {
        String query = "INSERT INTO " + TABLE_GRADE
                + " (" + COL_USER_ID + "," + COL_ESCAPE_ID + "," + COL_ENIGMA + "," + COL_GM + "," + COL_INM + "," + COL_HORROR + "," + COL_GLOBAL + ")"
                + " VALUES (:" + COL_USER_ID + ", :" + COL_ESCAPE_ID + ", :" + COL_ENIGMA + ", :" + COL_GM + ", :" + COL_INM + ", :" + COL_HORROR + ", :" + COL_GLOBAL + ")";
        Map<String, Object> parameters = getParametersUpdate(grade);
        parameters.put(COL_USER_ID, userId);
        parameters.put(COL_ESCAPE_ID, escapeId);
        namedParameterJdbcTemplate.update(query, parameters);
    }

    private Map<String, Object> getParametersUpdate(Grade grade) {
        Map<String,Object> parameters = new HashMap<>();
        parameters.put(COL_ENIGMA, grade.getEnigma());
        parameters.put(COL_GM, grade.getGameMaster());
        parameters.put(COL_INM, grade.getInmersion());
        parameters.put(COL_HORROR, grade.getHorror());
        parameters.put(COL_GLOBAL, grade.getGlobal());

        return parameters;
    }


}
