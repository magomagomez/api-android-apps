package com.magomez.androidapps.jctravels.outlets.controller;
import com.magomez.androidapps.jctravels.config.ApiConfig;
import com.magomez.androidapps.jctravels.outlets.dto.CreateOutletRequest;
import com.magomez.androidapps.jctravels.outlets.dto.OutletDTO;
import com.magomez.androidapps.jctravels.outlets.dto.OutletFilterRequest;
import com.magomez.androidapps.jctravels.outlets.service.OutletService;
import com.magomez.androidapps.jctravels.outlets.dto.UpdateOutletRequest;
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
@RequestMapping(value = ApiConfig.BASE_URL + "/outlets")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class OutletController {

    private final OutletService outletService;

    @Autowired
    public OutletController(OutletService outletService){

        this.outletService = outletService;
    }

    @GetMapping
    public List<OutletDTO> getOutlets(@Valid OutletFilterRequest requestFilter) {

        return outletService.getOutlets(requestFilter);
    }

    @GetMapping("{outletId}")
    public OutletDTO getOutlet(@PathVariable Integer outletId) {

        return outletService.getOutlet(outletId);
    }

    @PostMapping
    public void createOutlet(@RequestBody CreateOutletRequest request) {

        outletService.createOutlet(request);
    }

    @PutMapping("{outletId}")
    public void updateOutlet(@PathVariable Integer outletId,
                             @RequestBody UpdateOutletRequest request) {

        outletService.updateOutlet(outletId, request);
    }

}
