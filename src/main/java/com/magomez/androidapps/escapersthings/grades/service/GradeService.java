package com.magomez.androidapps.escapersthings.grades.service;

import com.magomez.androidapps.escapersthings.grades.dto.UpsertGradeRequest;
import com.magomez.androidapps.escapersthings.grades.dto.Grade;
import com.magomez.androidapps.escapersthings.grades.converter.GradeConverter;
import com.magomez.androidapps.escapersthings.grades.dao.GradeDao;
import com.magomez.androidapps.escapersthings.users.dao.UsersDao;
import com.magomez.androidapps.escapersthings.escaperooms.dto.UpdateEscapeRoomRequest;
import com.magomez.androidapps.escapersthings.escaperooms.service.EscapeRoomsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class GradeService {

    private final GradeDao gradeDao;
    private final UsersDao usersDao;
    private final EscapeRoomsService escapeRoomsService;

    @Autowired
    public GradeService(final GradeDao gradeDao, final UsersDao usersDao,
                        final EscapeRoomsService escapeRoomsService){
        this.gradeDao = gradeDao;
        this.usersDao = usersDao;
        this.escapeRoomsService = escapeRoomsService;
    }

    public List<UpsertGradeRequest> getGrades(Integer escapeId){
        List<Grade> grades = gradeDao.getGrades(escapeId);
        return GradeConverter.toDtoList(grades);
    }

    public void upsertGrades(Integer escapeRoomId, UpsertGradeRequest request){
        Integer userId = usersDao.getUserByUserName(request.nombre());
        Grade grade = gradeDao.getGradeByUserId(escapeRoomId,userId);
        if(grade != null){
            GradeConverter.fromDto(grade, request.grades());
            gradeDao.updateGrade(escapeRoomId,userId,grade);
        }
        else {
            Grade newGrade = GradeConverter.fromDto(request);
            gradeDao.insertGrade(escapeRoomId,userId,newGrade);
        }
        updateAverage(escapeRoomId);
    }

    private void updateAverage(Integer escapeRoomId){
        List<Grade> grades = gradeDao.getGrades(escapeRoomId);
        long globalUsers = grades.stream().filter(g -> g.getGlobal() != null).count();
        BigDecimal global = grades.stream().filter(g->g.getGlobal() != null)
                .map(Grade::getGlobal).reduce(BigDecimal.ZERO, BigDecimal::add);
        long horrorUsers = grades.stream().filter(g -> g.getHorror() != null).count();
        BigDecimal horror = grades.stream().filter(g->g.getHorror() != null)
                .map(Grade::getHorror).reduce(BigDecimal.ZERO, BigDecimal::add);
        if(globalUsers > 0 || (horrorUsers > 0)){
            UpdateEscapeRoomRequest request = new UpdateEscapeRoomRequest(
                    null,
                    null,
                    null,
                    null,
                    null,
                    globalUsers > 0 ? global.divide(BigDecimal.valueOf(globalUsers), 2, RoundingMode.HALF_UP):null,
                    horrorUsers > 0? horror.divide(BigDecimal.valueOf(horrorUsers), 2, RoundingMode.HALF_UP):null,
                    null,
                    null);
            escapeRoomsService.putEscapeRoom(escapeRoomId,request);
        }
    }

}
