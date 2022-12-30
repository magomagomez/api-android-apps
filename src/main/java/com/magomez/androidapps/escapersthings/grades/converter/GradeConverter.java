package com.magomez.androidapps.escapersthings.grades.converter;

import com.magomez.androidapps.escapersthings.utils.ConverterUtils;
import com.magomez.androidapps.escapersthings.grades.dto.UpsertGradeRequest;
import com.magomez.androidapps.escapersthings.grades.dto.Grade;
import com.magomez.androidapps.escapersthings.grades.dto.GradeDTO;

import java.util.List;

public class GradeConverter {

    private GradeConverter(){}

    public static List<UpsertGradeRequest> toDtoList(List<Grade> grades){
        return grades.stream()
                .map(GradeConverter::toDto)
                .toList();
    }

    public static UpsertGradeRequest toDto(Grade grade) {
        GradeDTO gradeDTO = new GradeDTO(
                grade.getEnigma(),
                grade.getGameMaster(),
                grade.getInmersion(),
                grade.getHorror(),
                grade.getGlobal());

        return new UpsertGradeRequest(
                grade.getName(),
                gradeDTO);
    }

    public static Grade fromDto(UpsertGradeRequest gradeDTO) {
        Grade grade = new Grade();
        grade.setEnigma(gradeDTO.grades().enigma());
        grade.setGameMaster(gradeDTO.grades().gameMaster());
        grade.setGlobal(gradeDTO.grades().global());
        grade.setInmersion(gradeDTO.grades().inmersion());
        grade.setHorror(gradeDTO.grades().terror());
        return grade;
    }

    public static void fromDto(Grade grade, GradeDTO gradeDTO) {
        ConverterUtils.updateField(grade::setEnigma,gradeDTO.enigma());
        ConverterUtils.updateField(grade::setGameMaster,gradeDTO.gameMaster());
        ConverterUtils.updateField(grade::setGlobal,gradeDTO.global());
        ConverterUtils.updateField(grade::setHorror,gradeDTO.terror());
        ConverterUtils.updateField(grade::setInmersion,gradeDTO.inmersion());
    }
}
