package com.magomez.androidapps.mustsee.users.mapper;

import com.magomez.androidapps.mustsee.users.dto.UserLogin;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserLoginMapper implements RowMapper<UserLogin> {

    @Override
    public UserLogin mapRow(ResultSet rs, int rowNum) throws SQLException{
        UserLogin userLogin = new UserLogin();
        userLogin.setId(rs.getInt("id"));
        userLogin.setName(rs.getString("name"));
        return userLogin;
    }
}
