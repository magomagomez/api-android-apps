package com.magomez.androidapps.friki.funkos.controller;
import com.magomez.androidapps.friki.config.ApiConfig;
import com.magomez.androidapps.friki.funkos.dto.CreateFunkoRequest;
import com.magomez.androidapps.friki.funkos.dto.FunkoDTO;
import com.magomez.androidapps.friki.funkos.dto.FunkoFilterRequest;
import com.magomez.androidapps.friki.funkos.service.FunkoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Funkos", description = "Endpoints de Funkos")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class FunkoController {

    private final FunkoService funkoService;

    @Autowired
    public FunkoController(FunkoService funkoService){
        this.funkoService = funkoService;
    }

    @Operation(summary = "Seach Funkos")
    @GetMapping
    public List<FunkoDTO> search(@Valid FunkoFilterRequest requestFilter) {

        return funkoService.search(requestFilter);
    }

    @Operation(summary = "Get Funko by id")
    @GetMapping("{funkoId}")
    public FunkoDTO getFunko(@PathVariable Integer funkoId) {

        return funkoService.getFunko(funkoId);
    }

    @Operation(summary = "Create Funko")
    @PostMapping
    public void createFunko(@RequestBody CreateFunkoRequest request) {

        funkoService.createFunko(request);
    }

}
