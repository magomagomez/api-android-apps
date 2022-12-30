package com.magomez.androidapps.jctravels.stores.service;

import com.magomez.androidapps.jctravels.stores.converter.StoreConverter;
import com.magomez.androidapps.jctravels.stores.dao.StoreDao;
import com.magomez.androidapps.jctravels.stores.dto.CreateStore;
import com.magomez.androidapps.jctravels.stores.dto.CreateStoreRequest;
import com.magomez.androidapps.jctravels.stores.dto.Store;
import com.magomez.androidapps.jctravels.stores.dto.StoreDTO;
import com.magomez.androidapps.jctravels.stores.dto.StoreFilter;
import com.magomez.androidapps.jctravels.stores.dto.StoreFilterRequest;
import com.magomez.androidapps.jctravels.stores.dto.UpdateStore;
import com.magomez.androidapps.jctravels.stores.dto.UpdateStoreRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StoreService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final StoreDao storeDao;

    @Autowired
    public StoreService(StoreDao storeDao){
        this.storeDao = storeDao;
    }

    public List<StoreDTO> getStores(StoreFilterRequest requestFilter){
        StoreFilter filter = StoreConverter.toFilter(requestFilter);
        List<Store> stores =  storeDao.getAllOutlets(filter);
        return StoreConverter.toDtoList(stores);
    }

    public StoreDTO getStore(Integer storeId){
        Store store =  storeDao.getStore(storeId);
        return StoreConverter.toDto(store);
    }

    public void createStore(CreateStoreRequest request){
        if(request == null || request.name() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        CreateStore store = StoreConverter.toRecord(request);
        storeDao.createStore(store);
    }

    public void updateStore(Integer storeId, UpdateStoreRequest request){
        if(request == null || (request.done() == null)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        UpdateStore store = StoreConverter.toRecord(request);
        storeDao.updateStore(storeId,store);
    }
}
