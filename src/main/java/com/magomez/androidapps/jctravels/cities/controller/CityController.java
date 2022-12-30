package com.magomez.androidapps.jctravels.cities.controller;
import com.magomez.androidapps.jctravels.cities.dto.CityDTO;
import com.magomez.androidapps.jctravels.cities.dto.CityFilterRequest;
import com.magomez.androidapps.jctravels.cities.dto.CreateCityRequest;
import com.magomez.androidapps.jctravels.cities.dto.UpdateCitytRequest;
import com.magomez.androidapps.jctravels.cities.service.CityService;
import com.magomez.androidapps.jctravels.config.ApiConfig;
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
@RequestMapping(value = ApiConfig.BASE_URL + "/cities")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class CityController {

    private final CityService cityService;

    @Autowired
    public CityController(CityService cityService){

        this.cityService = cityService;
    }

    @GetMapping
    public List<CityDTO> getCities(@Valid CityFilterRequest requestFilter) {

        return cityService.getCities(requestFilter);
    }

    @GetMapping("{cityId}")
    public CityDTO getCity(@PathVariable Integer cityId) {

        return cityService.getCity(cityId);
    }

    @PostMapping
    public void createCity(@RequestBody CreateCityRequest request) {

        cityService.createCity(request);
    }

    @PutMapping("{cityId}")
    public void updateCity(@PathVariable Integer cityId,
                               @RequestBody UpdateCitytRequest request) {

        cityService.updateCity(cityId, request);
    }

}
