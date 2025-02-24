package com.magomez.androidapps.friki.funkos.mapper;

import com.magomez.androidapps.friki.funkos.dto.Funko;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FunkoMapper implements RowMapper<Funko> {

    @Override
    public Funko mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Funko(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("image_path"),
                rs.getInt("number"),
                BooleanUtils.isTrue(rs.getInt("is_wishList") == 1),
                rs.getString("category"));
    }
}
