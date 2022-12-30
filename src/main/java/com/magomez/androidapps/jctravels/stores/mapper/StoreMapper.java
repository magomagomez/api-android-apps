package com.magomez.androidapps.jctravels.stores.mapper;

import com.magomez.androidapps.jctravels.stores.dto.Store;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StoreMapper implements RowMapper<Store> {

    @Override
    public Store mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Store(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("image_path"),
                rs.getBoolean("done"),
                rs.getInt("outlet")
        );
    }
}
