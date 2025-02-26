package com.magomez.androidapps.friki.funkos.service;

import com.magomez.androidapps.friki.funkos.converter.FunkoConverter;
import com.magomez.androidapps.friki.funkos.dao.FunkoDao;
import com.magomez.androidapps.friki.funkos.dto.CreateFunkoRequest;
import com.magomez.androidapps.friki.funkos.dto.Funko;
import com.magomez.androidapps.friki.funkos.dto.FunkoDTO;
import com.magomez.androidapps.friki.funkos.dto.FunkoFilterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FunkosService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final FunkoDao funkoDao;

    @Autowired
    public FunkosService(FunkoDao monumentDao){
        this.funkoDao = monumentDao;
    }

    public List<FunkoDTO> search(FunkoFilterRequest requestFilter){
        List<Funko> monuments =  funkoDao.search(requestFilter);
        return FunkoConverter.toDtoList(monuments);
    }

    public FunkoDTO getFunko(Integer funkoId){
        Funko funko =  funkoDao.getFunko(funkoId);
        return FunkoConverter.toDto(funko);
    }

    public void createFunko(CreateFunkoRequest request){
        if(request == null || request.name() == null || request.category() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        funkoDao.createFunko(request);
    }
}
