package com.magomez.androidapps.mustsee.users.dao;

import com.magomez.androidapps.mustsee.users.dto.LoginUser;
import com.magomez.androidapps.mustsee.users.dto.User;
import com.magomez.androidapps.mustsee.users.dto.UserLogin;
import com.magomez.androidapps.mustsee.users.dto.UserRecommend;
import com.magomez.androidapps.mustsee.users.mapper.UserLoginMapper;
import com.magomez.androidapps.mustsee.users.mapper.UserMapper;
import com.magomez.androidapps.mustsee.users.mapper.UserRecommendMapper;
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
        String query = "select name" + FROM + TABLE_USERS + " " + WHERE + " id = ?";
        return jdbcTemplate.queryForObject(query, String.class, userId);
    }

    public List<User> getRecommendationUserList(Integer userId) {
        String query = "Select tu.id, tu.name"
                + FROM + TABLE_USERS + " tu "
                + " inner join " + TABLE_MOVIES + " tm "
                + " on tu.id = tm.id_friend"
                + WHERE + " tm.id_user = ?";

        return jdbcTemplate.query(query, new UserMapper(), userId);
    }

    public List<User> getUserVisibility(Integer userId) {
        String query = "Select tu.id, tu.name"
                + FROM + TABLE_USERS + " tu "
                + " inner join " + TABLE_USERS_VISIBILITY + " tv "
                + " on tu.id = tv.id_friend"
                + WHERE + " tv.id_user = ?";

        return jdbcTemplate.query(query, new UserMapper(), userId);
    }

    public List<UserRecommend> getUserWithFilmRecommended(Integer filmId) {
        String query = "Select distinct (id_user), id_friend "
                + FROM + TABLE_MOVIES + " tm "
                + WHERE + " tm.id_external = ?";

        return jdbcTemplate.query(query, new UserRecommendMapper(), filmId);
    }

    public UserLogin loginUser(LoginUser user) {
        String query = "select * from " + TABLE_USERS
                + " where " + COL_USER_NAME + " = ? AND " + COL_PASSWORD + " = ?";
        return jdbcTemplate.queryForObject(query, new UserLoginMapper(), user.getName(), user.getPassword());
    }

}
