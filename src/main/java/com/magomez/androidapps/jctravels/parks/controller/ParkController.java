package com.magomez.androidapps.jctravels.parks.controller;

import com.magomez.androidapps.jctravels.config.ApiConfig;
import com.magomez.androidapps.jctravels.parks.dto.ParkDTO;
import com.magomez.androidapps.jctravels.parks.dto.ParkFilterRequest;
import com.magomez.androidapps.jctravels.parks.dto.CreateParkRequest;
import com.magomez.androidapps.jctravels.parks.dto.UpdateParkRequest;
import com.magomez.androidapps.jctravels.parks.service.ParkService;
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
import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL + "/parks")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class ParkController {

    private final ParkService parkService;

    @Autowired
    public ParkController(ParkService parkService){

        this.parkService = parkService;
    }

    @GetMapping
    public List<ParkDTO> getParks(@Valid ParkFilterRequest requestFilter) {

        return parkService.getParks(requestFilter);
    }

    @GetMapping("{parkId}")
    public ParkDTO getPark(@PathVariable Integer parkId) {

        return parkService.getPark(parkId);
    }

    @PostMapping
    public void createPark(@RequestBody CreateParkRequest request) {

        parkService.createPark(request);
    }

    @PutMapping("{parkId}")
    public void updatePark(@PathVariable Integer parkId,
                               @RequestBody UpdateParkRequest request) {

        parkService.updatePark(parkId, request);
    }

}
