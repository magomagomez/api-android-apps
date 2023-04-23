package com.magomez.androidapps.jctravels.travels.converter;

import com.magomez.androidapps.jctravels.travels.dto.CreateTravel;
import com.magomez.androidapps.jctravels.travels.dto.Travel;
import com.magomez.androidapps.jctravels.travels.dto.TravelDTO;
import com.magomez.androidapps.jctravels.travels.dto.CreateTravelRequest;

import java.util.List;

public class TravelConverter {

    private TravelConverter(){}

    public static List<TravelDTO> toDtoList(List<Travel> travels){
        return travels.stream()
                .map(TravelConverter::toDto)
                .toList();
    }

    public static TravelDTO toDto(Travel travel) {
        return new TravelDTO(
                travel.id(),
                travel.name()
        );
    }

    public static CreateTravel toRecord(CreateTravelRequest request) {
        return new CreateTravel(request.name());
    }

}
