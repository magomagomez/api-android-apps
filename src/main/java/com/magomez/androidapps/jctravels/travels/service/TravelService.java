package com.magomez.androidapps.jctravels.travels.service;

import com.magomez.androidapps.jctravels.travels.converter.TravelConverter;
import com.magomez.androidapps.jctravels.travels.dao.TravelDao;
import com.magomez.androidapps.jctravels.travels.dto.CreateTravel;
import com.magomez.androidapps.jctravels.travels.dto.Travel;
import com.magomez.androidapps.jctravels.travels.dto.TravelDTO;
import com.magomez.androidapps.jctravels.travels.dto.CreateTravelRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TravelService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final TravelDao travelDao;

    @Autowired
    public TravelService(TravelDao travelDao){
        this.travelDao = travelDao;
    }


    public List<TravelDTO> getTravels(){
        List<Travel> travels =  travelDao.getAllTravels();
        return TravelConverter.toDtoList(travels);
    }

    public TravelDTO getTravel(Integer travelId){
        Travel travel =  travelDao.getCity(travelId);
        return TravelConverter.toDto(travel);
    }

    public void createTravel(CreateTravelRequest request){
        if(request == null || request.name() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        CreateTravel travel = TravelConverter.toRecord(request);
        travelDao.createTravel(travel);
    }
}
