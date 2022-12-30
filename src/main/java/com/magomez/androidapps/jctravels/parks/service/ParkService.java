package com.magomez.androidapps.jctravels.parks.service;

import com.magomez.androidapps.jctravels.parks.converter.ParkConverter;
import com.magomez.androidapps.jctravels.parks.dto.ParkDTO;
import com.magomez.androidapps.jctravels.parks.dto.ParkFilterRequest;
import com.magomez.androidapps.jctravels.parks.dao.ParkDao;
import com.magomez.androidapps.jctravels.parks.dto.Park;
import com.magomez.androidapps.jctravels.parks.dto.ParkFilter;
import com.magomez.androidapps.jctravels.parks.dto.CreatePark;
import com.magomez.androidapps.jctravels.parks.dto.CreateParkRequest;
import com.magomez.androidapps.jctravels.parks.dto.UpdatePark;
import com.magomez.androidapps.jctravels.parks.dto.UpdateParkRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ParkService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final ParkDao parkDao;

    @Autowired
    public ParkService(ParkDao parkDao){

        this.parkDao = parkDao;
    }

    public List<ParkDTO> getParks(ParkFilterRequest requestFilter){
        ParkFilter filter = ParkConverter.toFilter(requestFilter);
        List<Park> parks =  parkDao.getAllParks(filter);
        return ParkConverter.toDtoList(parks);
    }

    public ParkDTO getPark(Integer parkId){
        Park park =  parkDao.getPark(parkId);
        return ParkConverter.toDto(park);
    }

    public void createPark(CreateParkRequest request){
        if(request == null || request.name() == null || request.city() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        CreatePark park = ParkConverter.toRecord(request);
        parkDao.createPark(park);
    }

    public void updatePark(Integer parkId, UpdateParkRequest request){
        if(request == null || (request.name() == null && request.city() == null)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        UpdatePark park = ParkConverter.toRecord(request);
        parkDao.updatePark(parkId,park);
    }
}
