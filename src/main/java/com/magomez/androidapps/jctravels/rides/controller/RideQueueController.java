package com.magomez.androidapps.jctravels.rides.controller;

import com.magomez.androidapps.jctravels.config.ApiConfig;
import com.magomez.androidapps.jctravels.rides.dto.ParkInfo;
import com.magomez.androidapps.jctravels.rides.service.RideQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL + "/wait-times")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class RideQueueController {

    private final RideQueueService rideQueueService;

    @Autowired
    public RideQueueController(RideQueueService rideQueueService){
        this.rideQueueService = rideQueueService;
    }

    @GetMapping
    public ParkInfo getRideQueueTimes(String id) throws IOException {
        return rideQueueService.getRideQueueTimes(id);
    }


}
