package com.magomez.androidapps.friki.funkos.controller;
import com.magomez.androidapps.friki.config.ApiConfig;
import com.magomez.androidapps.friki.funkos.dto.CreateFunkoRequest;
import com.magomez.androidapps.friki.funkos.dto.FunkoDTO;
import com.magomez.androidapps.friki.funkos.dto.FunkoFilterRequest;
import com.magomez.androidapps.friki.funkos.service.FunkosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL + "/funkos")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class FunkosController {

    private final FunkosService funkosService;

    @Autowired
    public FunkosController(FunkosService funkosService){
        this.funkosService = funkosService;
    }

    @GetMapping
    public List<FunkoDTO> getFunkos(@Valid FunkoFilterRequest requestFilter) {

        return funkosService.getFunkos(requestFilter);
    }

    @GetMapping("{funkoId}")
    public FunkoDTO getFunko(@PathVariable Integer funkoId) {

        return funkosService.getFunko(funkoId);
    }

    @PostMapping
    public void createFunko(@RequestBody CreateFunkoRequest request) {

        funkosService.createFunko(request);
    }

}
