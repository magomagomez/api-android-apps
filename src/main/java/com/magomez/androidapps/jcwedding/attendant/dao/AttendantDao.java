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

import java.util.ArrayList;
import java.util.Collections;
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
        String query = SELECT_ALL + FROM + TABLE_FRIENDS + " ";
        List<Object> args = new ArrayList<>();
        if (attendant != null && attendant.getName() != null && attendant.getSurname() != null) {
            query = query + WHERE + "name = ? AND surname = ?";
            args.add(getNameLowerCase(attendant));
            args.add(getSurnameLowerCase(attendant));
        }
        query = query + " ORDER BY id asc";

        return jdbcTemplate.query(query, new AttendantMapper(), args.toArray());
    }

    public List<Companion> searchCompanions(Integer id) {
        String query = SELECT_ALL + FROM + TABLE_FRIENDS + " wf "
                + " INNER JOIN " + TABLE_RELATIONS + " wr on wr.id_companion = wf.id"
                + " where wr.id_user = ?";

        return jdbcTemplate.query(query, new CompanionMapper(), id);
    }

    public void updateAttendants(RequestAttendantDTO requestAttendantDTO) {
        List<Integer> ids = requestAttendantDTO.getId();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String query = "UPDATE " + TABLE_FRIENDS + " SET attendance = 1 where id in (" + placeholders + ")";

        jdbcTemplate.update(query, ids.toArray());
    }

    public List<Attendant> getAttendantsByIds(RequestAttendantDTO requestAttendantDTO) {
        List<Integer> ids = requestAttendantDTO.getId();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String query = "Select * from " + TABLE_FRIENDS + " where id in (" + placeholders + ")";

        return jdbcTemplate.query(query, new AttendantMapper(), ids.toArray());
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
