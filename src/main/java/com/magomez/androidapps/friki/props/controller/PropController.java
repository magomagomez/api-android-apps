package com.magomez.androidapps.friki.props.controller;
import com.magomez.androidapps.friki.config.ApiConfig;
import com.magomez.androidapps.friki.props.dto.PropDTO;
import com.magomez.androidapps.friki.props.dto.PropFilterRequest;
import com.magomez.androidapps.friki.props.dto.CreatePropRequest;
import com.magomez.androidapps.friki.props.service.PropService;
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
@RequestMapping(value = ApiConfig.BASE_URL + "/props")
@Tag(name = "Props", description = "Endpoints de Props de Cine")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class PropController {

    private final PropService propsService;

    @Autowired
    public PropController(PropService propsService){
        this.propsService = propsService;
    }

    @Operation(summary = "Search Props")
    @GetMapping
    public List<PropDTO> search(@Valid PropFilterRequest requestFilter) {

        return propsService.search(requestFilter);
    }

    @Operation(summary = "Get Prop by Id")
    @GetMapping("{propId}")
    public PropDTO getProp(@PathVariable Integer propId) {

        return propsService.getProp(propId);
    }

    @Operation(summary = "Create Prop")
    @PostMapping
    public void createProp(@RequestBody CreatePropRequest request) {
        propsService.createProp(request);
    }

}
