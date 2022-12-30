package com.magomez.androidapps.jctravels.monuments.controller;
import com.magomez.androidapps.jctravels.config.ApiConfig;
import com.magomez.androidapps.jctravels.monuments.dto.CreateMonumentRequest;
import com.magomez.androidapps.jctravels.monuments.dto.MonumentDTO;
import com.magomez.androidapps.jctravels.monuments.dto.MonumentFilterRequest;
import com.magomez.androidapps.jctravels.monuments.dto.UpdateMonumentRequest;
import com.magomez.androidapps.jctravels.monuments.service.MonumentService;
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
@RequestMapping(value = ApiConfig.BASE_URL + "/monuments")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class MonumentController {

    private final MonumentService monumentService;

    @Autowired
    public MonumentController(MonumentService monumentService){

        this.monumentService = monumentService;
    }

    @GetMapping
    public List<MonumentDTO> getMonuments(@Valid MonumentFilterRequest requestFilter) {

        return monumentService.getMonuments(requestFilter);
    }

    @GetMapping("{monumentId}")
    public MonumentDTO getMonument(@PathVariable Integer monumentId) {

        return monumentService.getMonument(monumentId);
    }

    @PostMapping
    public void createMonument(@RequestBody CreateMonumentRequest request) {

        monumentService.createMonument(request);
    }

    @PutMapping("{monumentId}")
    public void updateMonument(@PathVariable Integer monumentId,
                               @RequestBody UpdateMonumentRequest request) {

        monumentService.updateMonument(monumentId, request);
    }

}
