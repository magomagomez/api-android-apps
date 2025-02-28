package com.magomez.androidapps.friki.props.mapper;

import com.magomez.androidapps.friki.props.dto.Prop;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class PropMapper implements RowMapper<Prop> {

    @Override
    public Prop mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Prop(
                rs.getInt("id"),
                rs.getString("name"),
                BooleanUtils.isTrue(rs.getInt("is_wishList") == 1),
                Arrays.asList(rs.getString("category").split(",", -1)));
    }
}
