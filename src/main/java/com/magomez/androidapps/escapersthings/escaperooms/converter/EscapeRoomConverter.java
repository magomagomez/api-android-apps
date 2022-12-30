package com.magomez.androidapps.escapersthings.escaperooms.converter;

import com.magomez.androidapps.escapersthings.escaperooms.dto.CreateEscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.dto.CreateEscapeRoomRequest;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomDTO;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomFilter;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoomFilterRequest;
import com.magomez.androidapps.escapersthings.escaperooms.dto.UpdateEscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.dto.UpdateEscapeRoomRequest;

import java.util.List;

public class EscapeRoomConverter {

    private EscapeRoomConverter(){}

    public static List<EscapeRoomDTO> toDtoList(List<EscapeRoom> escapeRooms){
        return escapeRooms.stream()
                .map(EscapeRoomConverter::toDto)
                .toList();
    }

    public static EscapeRoomDTO toDto(EscapeRoom escapeRoom) {
        return new EscapeRoomDTO(
                escapeRoom.id(),
                escapeRoom.name(),
                escapeRoom.imageUrl(),
                escapeRoom.done(),
                escapeRoom.ubication(),
                escapeRoom.type(),
                escapeRoom.totalNota(),
                escapeRoom.totalTerror(),
                escapeRoom.magoDone(),
                escapeRoom.crisDone()
        );
    }

    public static CreateEscapeRoom fromDto(CreateEscapeRoomRequest request) {
       return new CreateEscapeRoom(request.nombre(), request.imagenPath(), request.tipo());
    }

    public static UpdateEscapeRoom fromDto(EscapeRoom escapeRoom, UpdateEscapeRoomRequest request) {
        UpdateEscapeRoom updateEscapeRoom = new UpdateEscapeRoom();
        updateEscapeRoom.setDone(request.finalizado() != null ?request.finalizado():escapeRoom.done());
        updateEscapeRoom.setName(request.nombre() != null ?request.nombre():escapeRoom.name());
        updateEscapeRoom.setTotalTerror(request.terrorTotal() != null ?request.terrorTotal():escapeRoom.totalTerror());
        updateEscapeRoom.setType(request.tipo() != null ?request.tipo():escapeRoom.type());
        updateEscapeRoom.setTotalNota(request.notaTotal() != null ?request.notaTotal():escapeRoom.totalNota());
        updateEscapeRoom.setUbication(request.ciudad() != null ?request.ciudad():escapeRoom.ubication());
        updateEscapeRoom.setMagoDone(request.finalizadoJavi() != null ?request.finalizadoJavi():escapeRoom.magoDone());
        updateEscapeRoom.setCrisDone(request.finalizadoCris() != null ?request.finalizadoCris():escapeRoom.crisDone());
        return updateEscapeRoom;
    }

    public static EscapeRoomFilter fromDto(EscapeRoomFilterRequest filter) {
        return new EscapeRoomFilter(
                filter.finalizado(),
                filter.valoracion(),
                filter.tipo(),
                filter.order());
    }
}
