package com.magomez.androidapps.jctravels.rides.controller;

import com.magomez.androidapps.jctravels.config.ApiConfig;
import com.magomez.androidapps.jctravels.rides.dto.CreateRideRequest;
import com.magomez.androidapps.jctravels.rides.dto.RideDTO;
import com.magomez.androidapps.jctravels.rides.dto.RideFilterRequest;
import com.magomez.androidapps.jctravels.rides.dto.RidesDTO;
import com.magomez.androidapps.jctravels.rides.dto.UpdateRideRequest;
import com.magomez.androidapps.jctravels.rides.service.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL + "/rides")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class RideController {

    private final RideService rideService;

    @Autowired
    public RideController(RideService rideService){

        this.rideService = rideService;
    }

    @GetMapping
    public RidesDTO getRides(@Valid RideFilterRequest requestFilter) throws IOException {

        return rideService.getRides(requestFilter);
    }

    @GetMapping("{rideId}")
    public RideDTO getRide(@PathVariable Integer rideId){

        return rideService.getRide(rideId);
    }

    @PostMapping
    public void createRide(@RequestBody CreateRideRequest request) {

        rideService.createRide(request);
    }

    @PutMapping("{rideId}")
    public void updateRide(@PathVariable Integer rideId,
                            @RequestBody UpdateRideRequest request) {

        rideService.updateRide(rideId, request);
    }

}
