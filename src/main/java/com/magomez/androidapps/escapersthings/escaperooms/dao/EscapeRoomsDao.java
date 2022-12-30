package com.magomez.androidapps.escapersthings.escaperooms.dao;

import com.magomez.androidapps.escapersthings.escaperooms.dto.CreateEscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomFilter;
import com.magomez.androidapps.escapersthings.escaperooms.dto.UpdateEscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.mapper.EscapeRoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EscapeRoomsDao {

    private static final String COL_NAME = "name";
    private static final String COL_DONE = "done";
    private static final String COL_TOTAL_NOTA = "global_total";
    private static final String COL_TOTAL_TERROR = "terror_total";
    private static final String COL_JAVI_DONE = "javi_done";
    private static final String COL_CRIS_DONE = "cris_done";
    private static final String COL_TYPE = "type";
    private static final String COL_UBICATION = "ubication";

    private static final String TABLE_ESCAPE_ROOM = "escape_rooms";
    private static final String COL_ID = "id";

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<EscapeRoom> getAllEscapeRooms(EscapeRoomFilter filter) {
        String query = "select * ";
        query = query + "from " + TABLE_ESCAPE_ROOM + " ";
        String where = buildWhere(filter);
        query = query + where;
        return  jdbcTemplate.query(query, new EscapeRoomMapper());
    }

    public EscapeRoom getEscapeRoomById(Integer escapeId) {
        String query = "select * ";
        query = query + "from " + TABLE_ESCAPE_ROOM + " where " + COL_ID + " =" + escapeId;
        return  jdbcTemplate.queryForObject(query , new EscapeRoomMapper());
    }

    public List<EscapeRoom> getPendingEscapeRooms(Integer userId) {
        String query = "select * ";
        query = query + "from " + TABLE_ESCAPE_ROOM +
                " where " + COL_DONE + " = 1 and " + COL_ID + " not in("+
                "select ucn.escape_room_id from user_categoria_nota ucn where ucn.user_id = "+ userId +") " ;
        return  jdbcTemplate.query(query , new EscapeRoomMapper());
    }

    public void createEscapeRoom(CreateEscapeRoom escapeRoom) {
        String nextIdQuery = "select MAX(id) from " + TABLE_ESCAPE_ROOM ;
        Integer id = jdbcTemplate.queryForObject(nextIdQuery,Integer.class);

        String query = "INSERT INTO "+ TABLE_ESCAPE_ROOM + " (name,type,image_url) VALUES(?,?,?)";
        jdbcTemplate.update(query , escapeRoom.nombre(), escapeRoom.tipo(), (id+1) +".png");
    }

    public void updateEscapeRoom(UpdateEscapeRoom escapeRoom, Integer escapeId) {
        String query = "UPDATE " + TABLE_ESCAPE_ROOM + " set " +
            Stream.of(COL_NAME, COL_DONE, COL_TOTAL_NOTA, COL_TOTAL_TERROR, COL_TYPE, COL_UBICATION)
            .map(s-> s+" = :" + s)
            .collect(Collectors.joining(",")) +
            " WHERE " +
            COL_ID + " = " + escapeId;
        Map<String, Object> parameters = getParametersUpdate(escapeRoom);
        namedParameterJdbcTemplate.update(query, parameters);
    }

    private Map<String, Object> getParametersUpdate(UpdateEscapeRoom escapeRoom) {
        Map<String,Object> parameters = new HashMap<>();
        parameters.put(COL_NAME, escapeRoom.getName());
        parameters.put(COL_DONE, escapeRoom.getDone());
        parameters.put(COL_TOTAL_NOTA, escapeRoom.getTotalNota());
        parameters.put(COL_TOTAL_TERROR, escapeRoom.getTotalTerror());
        parameters.put(COL_TYPE, escapeRoom.getType());
        parameters.put(COL_JAVI_DONE,escapeRoom.getMagoDone());
        parameters.put(COL_CRIS_DONE,escapeRoom.getCrisDone());
        parameters.put(COL_UBICATION, escapeRoom.getUbication());
        return parameters;
    }

    private String buildWhere(EscapeRoomFilter filter){
            String query = "WHERE 1=1";

            if (filter.finalizado() != null) {
                if(filter.finalizado() == 0) {
                    query = query + " AND done = 0 AND javi_done is null AND cris_done is null";
                }
                else{
                    query = query + " AND done = 1 OR javi_done=1 OR cris_done = 1";
                }
            }
            if (filter.valoracion() != null) {
                switch(filter.valoracion()){
                    case 5:
                        query = query + " AND qe_valoration < 5 ";
                        break;
                    case 7:
                        query = query + " AND qe_valoration >= 5 AND qe_valoration < 7 ";
                        break;
                    case 9:
                        query = query + " AND qe_valoration >= 7 AND qe_valoration < 9 ";
                        break;
                    case 10:
                        query = query + " AND qe_valoration >= 9 ";
                        break;
                    default:
                        break;
                }
            }
            if (filter.tipo() != null && !filter.tipo().equals("ALL")) {
                query = query + " AND type = '" + filter.tipo() +"'";
            }
            if (filter.order() != null) {
                switch (filter.order()) {
                    case "NA":
                        query = query + " ORDER BY global_total ASC, terror_total ASC";
                        break;
                    case "ND":
                        query = query + " ORDER BY global_total DESC, terror_total DESC";
                        break;
                    case "TA":
                        query = query + " ORDER BY terror_total ASC, global_total ASC";
                        break;
                    case "TD":
                        query = query + " ORDER BY terror_total DESC, global_total DESC";
                        break;
                    default:
                        query = query + " ORDER BY name ASC";
                        break;
                }
            }
            else {
                query = query + " ORDER BY name ASC";
            }
        return query;
    }

}
