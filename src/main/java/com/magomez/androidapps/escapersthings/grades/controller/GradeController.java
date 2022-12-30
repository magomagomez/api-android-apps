package com.magomez.androidapps.escapersthings.grades.controller;

import com.magomez.androidapps.escapersthings.config.ApiConfig;
import com.magomez.androidapps.escapersthings.grades.service.GradeService;
import com.magomez.androidapps.escapersthings.grades.dto.UpsertGradeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConfig.BASE_URL + "/escapeRooms/{escapeRoomId}/grades")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST})
public class GradeController {

    private final GradeService gradeService;

    @Autowired
    public GradeController(final GradeService gradeService){
        this.gradeService = gradeService;
    }

    @GetMapping
    public List<UpsertGradeRequest> getGrades(@PathVariable Integer escapeRoomId) {
        return gradeService.getGrades(escapeRoomId);
    }

    @PostMapping
    public void upsertGrades(@PathVariable Integer escapeRoomId,
                             @RequestBody UpsertGradeRequest escapeGradesDTO) {
         gradeService.upsertGrades(escapeRoomId, escapeGradesDTO);
    }

}
