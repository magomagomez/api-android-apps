package com.magomez.androidapps.jctravels.stores.controller;

import com.magomez.androidapps.jctravels.config.ApiConfig;
import com.magomez.androidapps.jctravels.stores.service.StoreService;
import com.magomez.androidapps.jctravels.stores.dto.CreateStoreRequest;
import com.magomez.androidapps.jctravels.stores.dto.StoreDTO;
import com.magomez.androidapps.jctravels.stores.dto.StoreFilterRequest;
import com.magomez.androidapps.jctravels.stores.dto.UpdateStoreRequest;
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
@RequestMapping(value = ApiConfig.BASE_URL + "/stores")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class StoreController {

    private final StoreService storeService;

    @Autowired
    public StoreController(StoreService storeService){

        this.storeService = storeService;
    }

    @GetMapping
    public List<StoreDTO> getStores(@Valid StoreFilterRequest requestFilter){

        return storeService.getStores(requestFilter);
    }

    @GetMapping("{storeId}")
    public StoreDTO getStore(@PathVariable Integer storeId){

        return storeService.getStore(storeId);
    }

    @PostMapping
    public void createStore(@RequestBody CreateStoreRequest request) {

        storeService.createStore(request);
    }

    @PutMapping("{storeId}")
    public void updateStore(@PathVariable Integer storeId,
                            @RequestBody UpdateStoreRequest request) {

        storeService.updateStore(storeId, request);
    }

}
