package com.magomez.androidapps.escapersthings.v2.rooms;

import com.magomez.androidapps.escapersthings.v2.config.ApiException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomDao roomDao;

    public RoomService(RoomDao roomDao) {
        this.roomDao = roomDao;
    }

    /** Every room by name, with what each summary needs read in four queries in total. */
    public List<RoomSummary> list(int userId) {
        Map<Integer, List<RoomRows.Player>> players = roomDao.findAllPlayers().stream()
                .collect(Collectors.groupingBy(RoomRows.Player::roomId));
        Map<Integer, List<RoomRows.UserGradeRow>> grades = roomDao.findAllGrades().stream()
                .collect(Collectors.groupingBy(RoomRows.UserGradeRow::roomId));
        Map<Integer, List<RoomRows.Photo>> photos = roomDao.findAllPhotos().stream()
                .collect(Collectors.groupingBy(RoomRows.Photo::roomId));
        return roomDao.findAll().stream()
                .map(room -> RoomAssembler.summary(room,
                        players.getOrDefault(room.id(), List.of()),
                        grades.getOrDefault(room.id(), List.of()),
                        photos.getOrDefault(room.id(), List.of()),
                        userId))
                .toList();
    }

    public RoomDetail get(int id, int userId) {
        RoomRows.Room room = roomDao.findById(id).orElseThrow(ApiException::notFound);
        List<RoomRows.UserGradeRow> grades = roomDao.findGrades(id);
        RoomSummary summary = RoomAssembler.summary(room, roomDao.findPlayers(id), grades, roomDao.findPhotos(id), userId);
        return RoomAssembler.detail(summary, grades);
    }

    public RoomSummary summary(int id, int userId) {
        return get(id, userId).summary();
    }

    public RoomSummary create(SaveRoomRequest request, int userId) {
        int id = roomDao.insert(RoomValidator.room(request));
        return summary(id, userId);
    }

    public RoomSummary update(int id, SaveRoomRequest request, int userId) {
        SaveRoomRequest clean = RoomValidator.room(request);
        if (!roomDao.update(id, clean)) {
            throw ApiException.notFound();
        }
        return summary(id, userId);
    }

    /** Players, grades and photos go with it (ON DELETE CASCADE). */
    public void delete(int id) {
        if (!roomDao.delete(id)) {
            throw ApiException.notFound();
        }
    }

    /** 404 unless the room exists. */
    public void requireExists(int id) {
        if (!roomDao.exists(id)) {
            throw ApiException.notFound();
        }
    }
}
