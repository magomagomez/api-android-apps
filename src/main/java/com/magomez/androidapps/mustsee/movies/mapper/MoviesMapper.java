package com.magomez.androidapps.mustsee.movies.mapper;

import com.magomez.androidapps.mustsee.movies.dto.Film;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MoviesMapper implements RowMapper<Film> {

    @Override
    public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
        Film movie = new Film();
        movie.setId(rs.getInt("id"));
        movie.setTitle(rs.getString("name"));
        movie.setPosterUrl(rs.getString("image_url"));
        movie.setIdExternal(rs.getInt("id_external"));
        movie.setIdUser(rs.getInt("id_user"));
        movie.setIdFriend(rs.getInt("id_friend"));
        movie.setPlatform(rs.getString("platform"));
        movie.setType(rs.getInt("film_type"));
        return movie;
    }
}
