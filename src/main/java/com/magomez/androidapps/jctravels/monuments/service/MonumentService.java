package com.magomez.androidapps.jctravels.monuments.service;

import com.magomez.androidapps.jctravels.monuments.converter.MonumentConverter;
import com.magomez.androidapps.jctravels.monuments.dao.MonumentDao;
import com.magomez.androidapps.jctravels.monuments.dto.CreateMonument;
import com.magomez.androidapps.jctravels.monuments.dto.MonumentFilter;
import com.magomez.androidapps.jctravels.monuments.dto.UpdateMonument;
import com.magomez.androidapps.jctravels.monuments.dto.UpdateMonumentRequest;
import com.magomez.androidapps.jctravels.routes.dao.RouteDao;
import com.magomez.androidapps.jctravels.routes.dto.Route;
import com.magomez.androidapps.jctravels.monuments.dto.CreateMonumentRequest;
import com.magomez.androidapps.jctravels.monuments.dto.Monument;
import com.magomez.androidapps.jctravels.monuments.dto.MonumentDTO;
import com.magomez.androidapps.jctravels.monuments.dto.MonumentFilterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MonumentService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final MonumentDao monumentDao;
    private final RouteDao routeDao;

    @Autowired
    public MonumentService(MonumentDao monumentDao,RouteDao routeDao){
        this.monumentDao = monumentDao;
        this.routeDao = routeDao;
    }

    public List<MonumentDTO> getMonuments(MonumentFilterRequest requesFilter){
        MonumentFilter filter = MonumentConverter.toFilter(requesFilter);
        List<Monument> monuments =  monumentDao.getAllMonuments(filter);
        return MonumentConverter.toDtoList(monuments);
    }

    public MonumentDTO getMonument(Integer monumentId){
        Monument monument =  monumentDao.getMonument(monumentId);
        return MonumentConverter.toDto(monument);
    }

    public void createMonument(CreateMonumentRequest request){
        if(request == null || request.name() == null || request.city() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        CreateMonument monument = MonumentConverter.toRecord(request);
        Route defaultRoute = routeDao.getDefaultRoute(monument.city());
        monumentDao.createMonument(monument,defaultRoute.id());
    }

    public void updateMonument(Integer monumentId, UpdateMonumentRequest request){
        if(request == null || (request.done() == null && request.route() == null)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        UpdateMonument monument = MonumentConverter.toRecord(request);
        monumentDao.updateMonument(monumentId,monument);
    }
}
