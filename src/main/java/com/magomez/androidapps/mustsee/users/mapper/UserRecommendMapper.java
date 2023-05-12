package com.magomez.androidapps.mustsee.users.mapper;

import com.magomez.androidapps.mustsee.users.dto.UserRecommend;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRecommendMapper implements RowMapper<UserRecommend> {

    @Override
    public UserRecommend mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserRecommend user = new UserRecommend();
        user.setId(rs.getInt("id_user"));
        user.setFriendId(rs.getInt("id_friend"));
        return user;
    }
}
