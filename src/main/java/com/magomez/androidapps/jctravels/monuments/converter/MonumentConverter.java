package com.magomez.androidapps.jctravels.monuments.converter;

import com.magomez.androidapps.jctravels.monuments.dto.CreateMonument;
import com.magomez.androidapps.jctravels.monuments.dto.UpdateMonument;
import com.magomez.androidapps.jctravels.monuments.dto.CreateMonumentRequest;
import com.magomez.androidapps.jctravels.monuments.dto.Monument;
import com.magomez.androidapps.jctravels.monuments.dto.MonumentDTO;
import com.magomez.androidapps.jctravels.monuments.dto.MonumentFilter;
import com.magomez.androidapps.jctravels.monuments.dto.MonumentFilterRequest;
import com.magomez.androidapps.jctravels.monuments.dto.UpdateMonumentRequest;

import java.util.List;

public class MonumentConverter {

    private MonumentConverter(){}

    public static List<MonumentDTO> toDtoList(List<Monument> monuments){
        return monuments.stream()
                .map(MonumentConverter::toDto)
                .toList();
    }

    public static MonumentDTO toDto(Monument monument) {
        return new MonumentDTO(
                monument.id(),
                monument.name(),
                monument.imagePath(),
                monument.done(),
                monument.city(),
                monument.schedule(),
                monument.price(),
                monument.route(),
                monument.priority()
        );
    }

    public static CreateMonument toRecord(CreateMonumentRequest request) {
        return new CreateMonument(
                request.name(),
                request.city(),
                request.schedule(),
                request.price()
        );
    }

    public static UpdateMonument toRecord(UpdateMonumentRequest request) {
        return new UpdateMonument(
                request.done(),
                request.route()
        );
    }

    public static MonumentFilter toFilter(MonumentFilterRequest request){
        return new MonumentFilter(
                request.city(),
                request.route());
    }
}
