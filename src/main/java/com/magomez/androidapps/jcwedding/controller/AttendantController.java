package com.magomez.androidapps.jcwedding.controller;

import com.magomez.androidapps.jcwedding.config.ApiConfig;
import com.magomez.androidapps.jcwedding.dto.AttendantDTO;
import com.magomez.androidapps.jcwedding.dto.RequestAttendantDTO;
import com.magomez.androidapps.jcwedding.dto.SearchAttendantDTO;
import com.magomez.androidapps.jcwedding.service.AttendantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL+"/attendants")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class AttendantController {

    private final AttendantService attendantService;

    @Autowired
    public AttendantController(AttendantService attendantService){
        this.attendantService = attendantService;
    }

    @GetMapping
    public List<AttendantDTO> searchAttendants(
            @Valid SearchAttendantDTO attendant){
        return attendantService.searchAttendants(attendant);
    }

    @PostMapping
    public void updateAttendants(@RequestBody RequestAttendantDTO requestAttendantDTO){
        attendantService.updateAttendants(requestAttendantDTO);
    }

}
