package com.magomez.androidapps.jctravels.cities.service;

import com.magomez.androidapps.jctravels.cities.converter.CityConverter;
import com.magomez.androidapps.jctravels.cities.dto.CityDTO;
import com.magomez.androidapps.jctravels.cities.dto.UpdateCity;
import com.magomez.androidapps.jctravels.cities.dao.CityDao;
import com.magomez.androidapps.jctravels.cities.dto.CreateCity;
import com.magomez.androidapps.jctravels.cities.dto.CreateCityRequest;
import com.magomez.androidapps.jctravels.cities.dto.City;
import com.magomez.androidapps.jctravels.cities.dto.CityFilter;
import com.magomez.androidapps.jctravels.cities.dto.CityFilterRequest;
import com.magomez.androidapps.jctravels.cities.dto.UpdateCitytRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CityService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final CityDao cityDao;

    @Autowired
    public CityService(CityDao cityDao){
        this.cityDao = cityDao;
    }

    public List<CityDTO> getCities(CityFilterRequest requesFilter){
        CityFilter filter = CityConverter.toFilter(requesFilter);
        List<City> cities =  cityDao.getAllCities(filter);
        return CityConverter.toDtoList(cities);
    }

    public CityDTO getCity(Integer cityId){
        City city =  cityDao.getCity(cityId);
        return CityConverter.toDto(city);
    }

    public void createCity(CreateCityRequest request){
        if(request == null || request.name() == null || request.travel() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        CreateCity city = CityConverter.toRecord(request);
        cityDao.createCity(city);
    }

    public void updateCity(Integer cityId, UpdateCitytRequest request){
        if(request == null || (request.name() == null && request.travel() == null)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        UpdateCity city = CityConverter.toRecord(request);
        cityDao.updateCity(cityId,city);
    }
}
