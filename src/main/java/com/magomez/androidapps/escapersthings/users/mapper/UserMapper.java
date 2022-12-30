package com.magomez.androidapps.escapersthings.users.mapper;

import com.magomez.androidapps.escapersthings.users.dto.UserLogin;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper implements RowMapper<UserLogin> {

    @Override
    public UserLogin mapRow(ResultSet rs, int rowNum) throws SQLException{
        return new UserLogin(
                rs.getString("name"),
                rs.getInt("id"));
    }
}
