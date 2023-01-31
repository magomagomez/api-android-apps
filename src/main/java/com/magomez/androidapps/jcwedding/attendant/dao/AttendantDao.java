package com.magomez.androidapps.jcwedding.attendant.dao;

import com.magomez.androidapps.jcwedding.attendant.dto.Companion;
import com.magomez.androidapps.jcwedding.attendant.mapper.AttendantMapper;
import com.magomez.androidapps.jcwedding.attendant.mapper.CompanionMapper;
import com.magomez.androidapps.jcwedding.attendant.dto.Attendant;
import com.magomez.androidapps.jcwedding.attendant.dto.RequestAttendantDTO;
import com.magomez.androidapps.jcwedding.attendant.dto.SearchAttendantDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.FROM;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.SELECT_ALL;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.WHERE;

@Service
public class AttendantDao {

    private static final String TABLE_FRIENDS = "wedding_friends";
    private static final String TABLE_RELATIONS = "wedding_relations";

    private final JdbcTemplate jdbcTemplate;

    public AttendantDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Attendant> searchAttendants(SearchAttendantDTO attendant) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_FRIENDS + " ";
        query = buildWhere(attendant, query);
        query = query + " ORDER BY id asc";

        return  jdbcTemplate.query(query, new AttendantMapper());
    }

    public List<Companion> searchCompanions(Integer id) {

        String query = SELECT_ALL;
        query = query + FROM + TABLE_FRIENDS + " wf ";
        query = query + " INNER JOIN " + TABLE_RELATIONS + " wr on wr.id_companion = wf.id";
        query = query + " where wr.id_user = " + id;

        return  jdbcTemplate.query(query, new CompanionMapper());
    }

    public void updateAttendants(RequestAttendantDTO requestAttendantDTO) {

        String query = "UPDATE " + TABLE_FRIENDS;
        query = query + " SET attendance = 1";
        String ids = requestAttendantDTO.getId().toString();
        query = query + " where id in (" + ids.substring(1, ids.length()-1) + ")";

        jdbcTemplate.update(query);
    }

    private String buildWhere(SearchAttendantDTO attendant, String query) {
        if(attendant != null && attendant.getName() != null && attendant.getSurname() != null) {
            String attendantName = getNameLowerCase(attendant);
            String attendantSurname = getSurnameLowerCase(attendant);
            query = query + WHERE + "name =  '" + attendantName + "' AND surname = '" + attendantSurname +"'" ;
        }
        return query;
    }

    private String getSurnameLowerCase(SearchAttendantDTO attendant) {
        String surname = StringUtils.stripAccents(attendant.getSurname());
        return surname.toLowerCase();
    }

    private String getNameLowerCase(SearchAttendantDTO attendant) {
        String name = StringUtils.stripAccents(attendant.getName());
        String[] parts = name.split(" ");
        String attendantName = parts[0].toLowerCase();
        if(parts.length > 1){
            attendantName = attendantName + " " + parts[1].toLowerCase();
        }
        return attendantName;
    }

}
