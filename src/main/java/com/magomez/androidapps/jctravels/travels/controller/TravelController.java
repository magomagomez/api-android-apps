package com.magomez.androidapps.jctravels.travels.controller;
import com.magomez.androidapps.jctravels.config.ApiConfig;
import com.magomez.androidapps.jctravels.travels.dto.TravelDTO;
import com.magomez.androidapps.jctravels.travels.dto.CreateTravelRequest;
import com.magomez.androidapps.jctravels.travels.service.TravelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL + "/travels")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class TravelController {

    private final TravelService travelService;

    @Autowired
    public TravelController(TravelService travelService){

        this.travelService = travelService;
    }

    @GetMapping
    public List<TravelDTO> getTravels() {

        return travelService.getTravels();
    }

    @GetMapping("{travelId}")
    public TravelDTO getTravel(@PathVariable Integer travelId) {

        return travelService.getTravel(travelId);
    }

    @PostMapping
    public void createTravel(@RequestBody CreateTravelRequest request) {
        travelService.createTravel(request);
    }

}
