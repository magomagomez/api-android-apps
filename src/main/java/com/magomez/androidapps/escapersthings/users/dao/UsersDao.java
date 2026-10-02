package com.magomez.androidapps.escapersthings.users.dao;


import com.magomez.androidapps.escapersthings.users.mapper.UserMapper;
import com.magomez.androidapps.escapersthings.users.dto.LoginUser;
import com.magomez.androidapps.escapersthings.users.dto.UserLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UsersDao {

    private static final String TABLE_USERS = "users";
    private static final String COL_USER_NAME = "name";
    private static final String COL_PASSWORD = "password";

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;


    public Integer getUserByUserName(String name) {
        String query = "select id from " + TABLE_USERS + " where " + COL_USER_NAME + " = ?";
        return jdbcTemplate.queryForObject(query, Integer.class, name);
    }

    public UserLogin getUserById(Integer id) {
        String query = "select * from " + TABLE_USERS + " where id = ?";
        return jdbcTemplate.queryForObject(query, new UserMapper(), id);
    }

    public UserLogin loginUser(LoginUser user) {
        String query = "select * from " + TABLE_USERS + " where " + COL_USER_NAME + " = ? AND " + COL_PASSWORD + " = ?";
        return jdbcTemplate.queryForObject(query, new UserMapper(), user.nombre(), user.password());
    }

}
