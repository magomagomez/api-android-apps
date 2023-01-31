package com.magomez.androidapps.jcwedding.attendant.repository;

import com.magomez.androidapps.jcwedding.attendant.dao.AttendantDao;
import com.magomez.androidapps.jcwedding.attendant.dto.Companion;
import com.magomez.androidapps.jcwedding.attendant.dto.Attendant;
import com.magomez.androidapps.jcwedding.attendant.dto.RequestAttendantDTO;
import com.magomez.androidapps.jcwedding.attendant.dto.SearchAttendantDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendantRepository {

    private final AttendantDao attendantDao;

    @Autowired
    public AttendantRepository(AttendantDao attendantDao){
        this.attendantDao = attendantDao;
    }

    public List<Attendant> searchAttendants(SearchAttendantDTO attendant) {
        return attendantDao.searchAttendants(attendant);
    }

    public List<Companion> searchCompanions(Integer userId) {
        return attendantDao.searchCompanions(userId);
    }

    public void updateAttendants(RequestAttendantDTO requestAttendantDTO) {
        attendantDao.updateAttendants(requestAttendantDTO);
    }

}
