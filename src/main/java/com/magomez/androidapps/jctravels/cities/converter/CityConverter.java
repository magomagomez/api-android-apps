package com.magomez.androidapps.jctravels.cities.converter;

import com.magomez.androidapps.jctravels.cities.dto.CreateCity;
import com.magomez.androidapps.jctravels.cities.dto.CreateCityRequest;
import com.magomez.androidapps.jctravels.cities.dto.City;
import com.magomez.androidapps.jctravels.cities.dto.CityDTO;
import com.magomez.androidapps.jctravels.cities.dto.CityFilter;
import com.magomez.androidapps.jctravels.cities.dto.CityFilterRequest;
import com.magomez.androidapps.jctravels.cities.dto.UpdateCity;
import com.magomez.androidapps.jctravels.cities.dto.UpdateCitytRequest;

import java.util.List;

public class CityConverter {

    private CityConverter(){}

    public static List<CityDTO> toDtoList(List<City> cities){
        return cities.stream()
                .map(CityConverter::toDto)
                .toList();
    }

    public static CityDTO toDto(City city) {
        return new CityDTO(
                city.id(),
                city.name(),
                city.iconPath(),
                city.travel(),
                city.hasParks(),
                city.hasMonuments(),
                city.hasOutlets()
        );
    }

    public static CreateCity toRecord(CreateCityRequest request) {
        return new CreateCity(
                request.name(),
                request.travel()
        );
    }

    public static UpdateCity toRecord(UpdateCitytRequest request) {
        return new UpdateCity(
                request.name(),
                request.travel()
        );
    }

    public static CityFilter toFilter(CityFilterRequest request){
        return new CityFilter(
                request.travel());
    }
}
