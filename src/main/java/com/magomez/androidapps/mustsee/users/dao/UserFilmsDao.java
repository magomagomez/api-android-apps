package com.magomez.androidapps.mustsee.users.dao;

import com.magomez.androidapps.mustsee.users.dto.LoginUser;
import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.dto.UserLogin;
import com.magomez.androidapps.mustsee.users.mapper.UserLoginMapper;
import com.magomez.androidapps.mustsee.users.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.FROM;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.WHERE;

@Service
public class UserFilmsDao {

    private static final String TABLE_USERS = "films_users";
    private static final String TABLE_USERS_VISIBILITY = "films_users_visibility";
    private static final String TABLE_MOVIES = "films_recommend";
    private static final String COL_USER_NAME = "name";
    private static final String COL_PASSWORD = "password";

    private final JdbcTemplate jdbcTemplate;

    public UserFilmsDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getUserName(Integer userId) {
        String query = "name";
        query = query + FROM + TABLE_USERS + " ";
        query = query + WHERE + " id_user =  " + userId;

        return  jdbcTemplate.queryForObject(query, String.class);
    }

    public List<User> getRecommendationUserList(Integer userId) {

        String query = "Select tu.id, tu.name";
        query = query + FROM + TABLE_USERS + " tu ";
        query = query + " inner join " + TABLE_MOVIES + " tm ";
        query = query + " on tu.id = tm.id_friend";
        query = query + WHERE + " tm.id_user =  " + userId;

        return  jdbcTemplate.query(query, new UserMapper());
    }

    public List<User> getUserVisibility(Integer userId) {

        String query = "Select tu.id, tu.name";
        query = query + FROM + TABLE_USERS + " tu ";
        query = query + " inner join " + TABLE_USERS_VISIBILITY + " tv ";
        query = query + " on tu.id = tv.id_friend";
        query = query + WHERE + " tv.id_user =  " + userId;

        return  jdbcTemplate.query(query, new UserMapper());
    }

    public List<Integer> getUserWithFilmRecommended(Integer filmId) {

        String query = "Select distinct (id_user)";
        query = query + FROM + TABLE_MOVIES + " tm ";
        query = query + WHERE + " tm.id_external =  " + filmId;

        return  jdbcTemplate.queryForList(query, Integer.class);
    }


    public UserLogin loginUser(LoginUser user) {
        String query = "select * from " + TABLE_USERS;
        query = query + " where " + COL_USER_NAME + " = '" + user.getName() +"' AND "
                + COL_PASSWORD + " = '" + user.getPassword() + "'" ;
        return  jdbcTemplate.queryForObject(query, new UserLoginMapper());
    }

}
