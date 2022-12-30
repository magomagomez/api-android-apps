package com.magomez.androidapps.escapersthings.escaperooms.mapper;


import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoom;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EscapeRoomMapper implements RowMapper<EscapeRoom> {

    @Override
    public EscapeRoom mapRow(ResultSet rs, int rowNum) throws SQLException{
        return new EscapeRoom(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("image_url"),
                rs.getInt("done"),
                rs.getString("ubication"),
                rs.getString("type"),
                rs.getBigDecimal("global_total"),
                rs.getBigDecimal("terror_total"),
                rs.getInt("javi_done"),
                rs.getInt("cris_done"));
    }
}
