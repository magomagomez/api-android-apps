package com.magomez.androidapps.jctravels.outlets.service;

import com.magomez.androidapps.jctravels.outlets.dto.UpdateOutletRequest;
import com.magomez.androidapps.jctravels.outlets.converter.OutletConverter;
import com.magomez.androidapps.jctravels.outlets.dao.OutletDao;
import com.magomez.androidapps.jctravels.outlets.dto.CreateOutlet;
import com.magomez.androidapps.jctravels.outlets.dto.CreateOutletRequest;
import com.magomez.androidapps.jctravels.outlets.dto.Outlet;
import com.magomez.androidapps.jctravels.outlets.dto.OutletDTO;
import com.magomez.androidapps.jctravels.outlets.dto.OutletFilter;
import com.magomez.androidapps.jctravels.outlets.dto.OutletFilterRequest;
import com.magomez.androidapps.jctravels.outlets.dto.UpdateOutlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OutletService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final OutletDao outletDao;

    @Autowired
    public OutletService(OutletDao outletDao){

        this.outletDao = outletDao;
    }

    public List<OutletDTO> getOutlets(OutletFilterRequest requestFilter){
        OutletFilter filter = OutletConverter.toFilter(requestFilter);
        List<Outlet> outlets =  outletDao.getAllOutlets(filter);
        return OutletConverter.toDtoList(outlets);
    }

    public OutletDTO getOutlet(Integer outletId){
        Outlet outlet =  outletDao.getOutlet(outletId);
        return OutletConverter.toDto(outlet);
    }

    public void createOutlet(CreateOutletRequest request){
        if(request == null || request.name() == null || request.city() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        CreateOutlet outlet = OutletConverter.toRecord(request);
        outletDao.createOutlet(outlet);
    }

    public void updateOutlet(Integer outletId, UpdateOutletRequest request){
        if(request == null || (request.name() == null && request.city() == null)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        UpdateOutlet outlet = OutletConverter.toRecord(request);
        outletDao.updateOutlet(outletId,outlet);
    }
}
