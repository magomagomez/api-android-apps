package com.magomez.androidapps.escapersthings.escaperooms.service;

import com.magomez.androidapps.escapersthings.escaperooms.dto.UsersEnum;
import com.magomez.androidapps.escapersthings.escaperooms.converter.EscapeRoomConverter;
import com.magomez.androidapps.escapersthings.escaperooms.dao.EscapeRoomsDao;
import com.magomez.androidapps.escapersthings.escaperooms.dto.CreateEscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.dto.CreateEscapeRoomRequest;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomDTO;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomFilter;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomFilterRequest;
import com.magomez.androidapps.escapersthings.escaperooms.dto.UpdateEscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.dto.UpdateEscapeRoomRequest;
import com.magomez.androidapps.escapersthings.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EscapeRoomsService {

    private final EscapeRoomsDao escapeRoomsDao;
    private final MailService mailService;

    @Autowired
    public EscapeRoomsService (final EscapeRoomsDao escapeRoomsDao, final MailService mailService){
        this.escapeRoomsDao = escapeRoomsDao;
        this.mailService = mailService;
    }

    public List<EscapeRoomDTO> getAllEscapeRooms(EscapeRoomFilterRequest request){
        EscapeRoomFilter escapeRoom = EscapeRoomConverter.fromDto(request);
        List<EscapeRoom> escapeRooms = escapeRoomsDao.getAllEscapeRooms(escapeRoom);
        return EscapeRoomConverter.toDtoList(escapeRooms);
    }

    public EscapeRoomDTO getEscapeRoom(Integer escapeId){
        EscapeRoom escapeRoom = escapeRoomsDao.getEscapeRoomById(escapeId);
        return EscapeRoomConverter.toDto(escapeRoom);
    }

    public void putEscapeRoom(Integer escapeId, UpdateEscapeRoomRequest request){
        EscapeRoom escapeRoom = escapeRoomsDao.getEscapeRoomById(escapeId);
        UpdateEscapeRoom updateEscapeRoom = EscapeRoomConverter.fromDto(escapeRoom,request);
        escapeRoomsDao.updateEscapeRoom(updateEscapeRoom,escapeId);
        sendMails(escapeRoom, request);
    }

    public void postEscapeRoom(CreateEscapeRoomRequest request){
        CreateEscapeRoom escapeRoom = EscapeRoomConverter.fromDto(request);
        escapeRoomsDao.createEscapeRoom(escapeRoom);
    }

    private void sendMails(EscapeRoom escapeRoom, UpdateEscapeRoomRequest request) {
        if(Boolean.TRUE.equals(isTeamFinished(request))){
            sendMail(UsersEnum.JAVI, escapeRoom);
            sendMail(UsersEnum.CRIS, escapeRoom);
            sendMail(UsersEnum.URI, escapeRoom);
            sendMail(UsersEnum.ALEX, escapeRoom);
        }
        else if (Boolean.TRUE.equals(isCrisFinished(request))){
            sendMail(UsersEnum.CRIS, escapeRoom);
        }
        else if (Boolean.TRUE.equals(isJaviFinished(request))){
            sendMail(UsersEnum.JAVI, escapeRoom);
        }
    }

    private Boolean isTeamFinished(UpdateEscapeRoomRequest escapeRoom){
        return escapeRoom.finalizado() != null && escapeRoom.finalizado().equals(1);
    }

    private Boolean isCrisFinished(UpdateEscapeRoomRequest escapeRoom){
        return escapeRoom.finalizadoCris() != null && escapeRoom.finalizadoCris().equals(1);
    }

    private Boolean isJaviFinished(UpdateEscapeRoomRequest escapeRoom){
        return escapeRoom.finalizadoJavi() != null && escapeRoom.finalizadoJavi().equals(1);
    }

    private void sendMail(UsersEnum user, EscapeRoom escapeRoom){
        mailService.sendMail(user.getMail(),escapeRoom.name(),escapeRoom.id(),
                escapeRoom.imageUrl(), user.getUserName());
    }

}
