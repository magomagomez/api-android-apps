package com.magomez.androidapps.jcwedding.attendant.converter;

import com.magomez.androidapps.jcwedding.attendant.dto.Attendant;
import com.magomez.androidapps.jcwedding.attendant.dto.AttendantDTO;
import com.magomez.androidapps.jcwedding.attendant.dto.Companion;
import com.magomez.androidapps.jcwedding.attendant.dto.CompanionDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class AttendantConverter {

    private AttendantConverter(){}

    public static List<AttendantDTO> toDto(List<Attendant> source) {
        return source.stream().map(AttendantConverter::toDto).toList();
    }

    public static AttendantDTO toDto(Attendant source) {
        AttendantDTO dto = new AttendantDTO();
        dto.setId(source.getId());
        dto.setName(getNameCapitalize(source.getName()));
        dto.setSurname(getSurnameCapitalize(source.getSurname()));
        dto.setSpecialFeatures(source.getSpecialFeatures());
        if(source.getAttendance() != null) {
            dto.setAttendance(source.getAttendance().equals(1));
        }
        dto.setCompanion(toCompanionDTO(source.getCompanion()));
        return dto;
    }

    private static List<CompanionDTO> toCompanionDTO(List<Companion> source) {
        return source.stream().map(AttendantConverter::toDto).toList();
    }

    private static CompanionDTO toDto(Companion source) {
        CompanionDTO dto = new CompanionDTO();
        dto.setId(source.getId());
        dto.setName(getNameCapitalize(source.getName()));
        if(source.getAttendance() != null) {
            dto.setAttendance(source.getAttendance().equals(1));
        }
        return dto;
    }

    private static String getSurnameCapitalize(String surname) {
        return StringUtils.capitalize(surname);
    }

    private static String getNameCapitalize(String name) {
        String attendantName = "";
        String[] parts = name.split(" ");
        attendantName = StringUtils.capitalize(parts[0]);
        if(parts.length > 1){
            attendantName = attendantName + " " + StringUtils.capitalize(parts[1]);
        }
        return attendantName;
    }

}
