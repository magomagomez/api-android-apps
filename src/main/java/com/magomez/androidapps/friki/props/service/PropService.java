package com.magomez.androidapps.friki.props.service;

import com.magomez.androidapps.friki.props.converter.PropConverter;
import com.magomez.androidapps.friki.props.dao.PropDao;
import com.magomez.androidapps.friki.props.dto.Prop;
import com.magomez.androidapps.friki.props.dto.PropDTO;
import com.magomez.androidapps.friki.props.dto.PropFilterRequest;
import com.magomez.androidapps.friki.props.dto.CreatePropRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PropService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final PropDao propDao;

    @Autowired
    public PropService(PropDao propDao){
        this.propDao = propDao;
    }

    public List<PropDTO> search(PropFilterRequest requestFilter){
        List<Prop> monuments =  propDao.search(requestFilter);
        return PropConverter.toDtoList(monuments);
    }

    public PropDTO getProp(Integer propId){
        Prop prop =  propDao.getProp(propId);
        return PropConverter.toDto(prop);
    }

    public void createProp(CreatePropRequest request){
        if(request == null || request.name() == null || request.category() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        propDao.createProps(request);
    }
}
