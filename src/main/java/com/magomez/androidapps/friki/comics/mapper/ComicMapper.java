package com.magomez.androidapps.friki.comics.mapper;

import com.magomez.androidapps.friki.comics.dto.Comic;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class ComicMapper implements RowMapper<Comic> {

    @Override
    public Comic mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Comic(
                rs.getInt("id"),
                rs.getString("name"),
                BooleanUtils.isTrue(rs.getInt("is_wishList") == 1),
                Arrays.asList(rs.getString("category").split(",", -1)));
    }
}
