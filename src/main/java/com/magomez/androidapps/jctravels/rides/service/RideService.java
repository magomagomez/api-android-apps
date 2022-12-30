package com.magomez.androidapps.jctravels.rides.service;

import com.magomez.androidapps.jctravels.parks.dao.ParkDao;
import com.magomez.androidapps.jctravels.parks.dto.Park;
import com.magomez.androidapps.jctravels.rides.converter.RideConverter;
import com.magomez.androidapps.jctravels.rides.dao.RideDao;
import com.magomez.androidapps.jctravels.rides.dto.CreateRide;
import com.magomez.androidapps.jctravels.rides.dto.CreateRideRequest;
import com.magomez.androidapps.jctravels.rides.dto.ParkInfo;
import com.magomez.androidapps.jctravels.rides.dto.Ride;
import com.magomez.androidapps.jctravels.rides.dto.RideDTO;
import com.magomez.androidapps.jctravels.rides.dto.RideFilter;
import com.magomez.androidapps.jctravels.rides.dto.RideFilterRequest;
import com.magomez.androidapps.jctravels.rides.dto.RideInfo;
import com.magomez.androidapps.jctravels.rides.dto.RidesDTO;
import com.magomez.androidapps.jctravels.rides.dto.UpdateRide;
import com.magomez.androidapps.jctravels.rides.dto.UpdateRideRequest;
import com.magomez.androidapps.jctravels.rides.dto.WaitTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RideService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final RideDao rideDao;
    private final ParkDao parkDao;
    private final RideQueueService rideQueueService;

    @Autowired
    public RideService(RideDao rideDao, RideQueueService rideQueueService, ParkDao parkDao){
        this.rideDao = rideDao;
        this.rideQueueService = rideQueueService;
        this.parkDao = parkDao;
    }

    public RidesDTO getRides(RideFilterRequest requestFilter) throws IOException {
        if(requestFilter == null || requestFilter.park() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        RideFilter filter = RideConverter.toFilter(requestFilter);
        List<Ride> rides =  rideDao.getAllRides(filter);
        Park park = parkDao.getPark(requestFilter.park());
        ParkInfo parkInfo = rideQueueService.getRideQueueTimes(park.queueId());
        List<RideDTO> must = new ArrayList<>();
        List<RideDTO> maybe = new ArrayList<>();
        for (Ride ride : rides){
            RideDTO rideDTO = RideConverter.toDto(ride);
            if(parkInfo.getLiveData() != null) {
                getRideTime(ride.code(), rideDTO, parkInfo.getLiveData());
            }
            if(Boolean.TRUE.equals(ride.must())){
                must.add(rideDTO);
            }
            else{
                maybe.add(rideDTO);
            }
        }
        return new RidesDTO(must,maybe);
    }

    public RideDTO getRide(Integer rideId) {
        try {
            Ride ride = rideDao.getRide(rideId);
            Park park = parkDao.getPark(ride.park());
            ParkInfo parkInfo = rideQueueService.getRideQueueTimes(park.queueId());
            RideDTO rideDTO = RideConverter.toDto(ride);
            if(parkInfo.getLiveData() != null) {
                getRideTime(ride.code(), rideDTO, parkInfo.getLiveData());
            }
            return rideDTO;
        }catch (IOException ex){
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    private void getRideTime(String rideCode, RideDTO rideDTO, List<RideInfo> ridesTime ) {
        for (RideInfo rideTime : ridesTime) {
            if (rideTime.getId().equals(rideCode)) {
                getWaitTimes(rideDTO, rideTime);
                getForecast(rideDTO, rideTime);
                getShowTimes(rideDTO, rideTime);
                rideDTO.setStatus(rideTime.getStatus());
                rideDTO.setEntityType(rideTime.getEntityType());
            }
        }
    }

    private void getShowTimes(RideDTO rideDTO, RideInfo rideTime) {
        if(rideTime.getShowtimes() != null) {
            rideDTO.setShowtimes(RideConverter.toShowTimeList(rideTime.getShowtimes()));
        }
    }

    private void getWaitTimes(RideDTO rideDTO, RideInfo rideTime) {
        if(rideTime.getQueue() != null && rideTime.getQueue().getStandby() != null) {
            rideDTO.setQueueTime(rideTime.getQueue().getStandby().getWaitTime());
        }
    }

    private void getForecast(RideDTO rideDTO, RideInfo rideTime) {
        if(rideTime.getForecast() != null) {
            List<WaitTime> filteredForecast = new ArrayList<>();
            for (WaitTime waitTime : rideTime.getForecast()){
                Date now = Date.from(Instant.now());
                if(now.before(waitTime.getTime())){
                    filteredForecast.add(waitTime);
                }
            }
            rideDTO.setForecast(RideConverter.toWaitTimeList(filteredForecast));
        }
    }

    public void createRide(CreateRideRequest request){
        if(request == null || request.name() == null || request.park() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        CreateRide ride = RideConverter.toRecord(request);
        rideDao.createRide(ride);
    }

    public void updateRide(Integer rideId, UpdateRideRequest request){
        if(request == null || (request.name() == null && request.park() == null && request.done() == null)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        UpdateRide ride = RideConverter.toRecord(request);
        rideDao.updateRide(rideId,ride);
    }
}
