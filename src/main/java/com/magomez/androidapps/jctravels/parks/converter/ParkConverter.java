package com.magomez.androidapps.jctravels.parks.converter;

import com.magomez.androidapps.jctravels.parks.dto.Park;
import com.magomez.androidapps.jctravels.parks.dto.ParkDTO;
import com.magomez.androidapps.jctravels.parks.dto.ParkFilter;
import com.magomez.androidapps.jctravels.parks.dto.ParkFilterRequest;
import com.magomez.androidapps.jctravels.parks.dto.CreatePark;
import com.magomez.androidapps.jctravels.parks.dto.CreateParkRequest;
import com.magomez.androidapps.jctravels.parks.dto.UpdatePark;
import com.magomez.androidapps.jctravels.parks.dto.UpdateParkRequest;

import java.util.List;

public class ParkConverter {

    private ParkConverter(){}

    public static List<ParkDTO> toDtoList(List<Park> parks){
        return parks.stream()
                .map(ParkConverter::toDto)
                .toList();
    }

    public static ParkDTO toDto(Park park) {
        return new ParkDTO(
                park.id(),
                park.name(),
                park.queueId(),
                park.city()
        );
    }

    public static CreatePark toRecord(CreateParkRequest request) {
        return new CreatePark(
                request.name(),
                request.city()
        );
    }

    public static UpdatePark toRecord(UpdateParkRequest request) {
        return new UpdatePark(
                request.name(),
                request.city()
        );
    }

    public static ParkFilter toFilter(ParkFilterRequest request){
        return new ParkFilter(
                request.city()
        );
    }
}
