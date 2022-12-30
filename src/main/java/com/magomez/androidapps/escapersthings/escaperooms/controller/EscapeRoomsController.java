package com.magomez.androidapps.escapersthings.escaperooms.controller;

import com.magomez.androidapps.escapersthings.config.ApiConfig;
import com.magomez.androidapps.escapersthings.escaperooms.dto.CreateEscapeRoomRequest;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomDTO;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomFilterRequest;
import com.magomez.androidapps.escapersthings.escaperooms.dto.UpdateEscapeRoomRequest;
import com.magomez.androidapps.escapersthings.escaperooms.service.EscapeRoomsService;
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
@RequestMapping(ApiConfig.BASE_URL + "/escapeRooms")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET, RequestMethod.POST,RequestMethod.PUT})
public class EscapeRoomsController {

    private final EscapeRoomsService escapeRoomsService;

    @Autowired
    public EscapeRoomsController (final EscapeRoomsService escapeRoomsService){
        this.escapeRoomsService = escapeRoomsService;
    }

    @GetMapping
    public List<EscapeRoomDTO> getEscapeRooms(@Valid EscapeRoomFilterRequest requestFilter) {
        return escapeRoomsService.getAllEscapeRooms(requestFilter);
    }

    @GetMapping("{escapeRoomId}")
    public EscapeRoomDTO getEscapeRoom(@PathVariable Integer escapeRoomId) {
        return escapeRoomsService.getEscapeRoom(escapeRoomId);
    }

    @PostMapping
    public void postEscapeRoom(@RequestBody CreateEscapeRoomRequest request){
        escapeRoomsService.postEscapeRoom(request);
    }

    @PutMapping("{escapeRoomId}")
    public void putEscapeRoom(@PathVariable Integer escapeRoomId,
                              @RequestBody UpdateEscapeRoomRequest request){
        escapeRoomsService.putEscapeRoom(escapeRoomId,request);
    }


}
