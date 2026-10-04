package com.magomez.androidapps.jctravels.parks.service;

import com.magomez.androidapps.jctravels.parks.converter.ParkConverter;
import com.magomez.androidapps.jctravels.parks.dto.ParkDTO;
import com.magomez.androidapps.jctravels.parks.dto.ParkFilterRequest;
import com.magomez.androidapps.jctravels.cities.dao.CityDao;
import com.magomez.androidapps.jctravels.cities.dto.CityContent;
import com.magomez.androidapps.jctravels.parks.dao.ParkDao;
import com.magomez.androidapps.jctravels.parks.dto.Park;
import com.magomez.androidapps.jctravels.parks.dto.ParkFilter;
import com.magomez.androidapps.jctravels.parks.dto.CreatePark;
import com.magomez.androidapps.jctravels.parks.dto.CreateParkRequest;
import com.magomez.androidapps.jctravels.parks.dto.UpdatePark;
import com.magomez.androidapps.jctravels.parks.dto.UpdateParkRequest;
import com.magomez.androidapps.jctravels.parks.dto.ParkHoursDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

@Service
public class ParkService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private static final Logger logger = Logger.getLogger(ParkService.class.getName());
    private final ParkDao parkDao;
    private final CityDao cityDao;
    private final ParkScheduleService scheduleService;

    @Autowired
    public ParkService(ParkDao parkDao, CityDao cityDao, ParkScheduleService scheduleService){
        this.parkDao = parkDao;
        this.cityDao = cityDao;
        this.scheduleService = scheduleService;
    }

    public List<ParkDTO> getParks(ParkFilterRequest requestFilter){
        ParkFilter filter = ParkConverter.toFilter(requestFilter);
        List<Park> parks =  parkDao.getAllParks(filter);
        return parks.stream().map(park -> ParkConverter.toDto(park, hoursOf(park))).toList();
    }

    public ParkDTO getPark(Integer parkId){
        Park park =  parkDao.getPark(parkId);
        return ParkConverter.toDto(park, hoursOf(park));
    }

    /** Opening hours are a nice-to-have: if themeparks.wiki fails, the park is still listed. */
    private ParkHoursDTO hoursOf(Park park) {
        if (park.queueId() == null) {
            return null;
        }
        try {
            return ParkHours.next(scheduleService.schedule(park.queueId()), Instant.now());
        } catch (IOException ex) {
            logger.warning(() -> "No schedule for park " + park.id() + ": " + ex.getMessage());
            return null;
        }
    }

    @Transactional
    public void createPark(CreateParkRequest request){
        if(request == null || request.name() == null || request.city() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        CreatePark park = ParkConverter.toRecord(request);
        parkDao.createPark(park);
        cityDao.markHas(park.city(), CityContent.PARKS);
    }

    public void updatePark(Integer parkId, UpdateParkRequest request){
        if(request == null || (request.name() == null && request.city() == null)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        UpdatePark park = ParkConverter.toRecord(request);
        parkDao.updatePark(parkId,park);
    }
}
