package com.magomez.androidapps.escapersthings.v2.rooms;

import com.magomez.androidapps.escapersthings.v2.auth.UserDao;
import com.magomez.androidapps.escapersthings.v2.auth.UserDTO;
import com.magomez.androidapps.escapersthings.v2.config.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** "Hemos jugado" and "Puntuar": the players of a room and my own grade. */
@Service
public class PlayService {

    private static final String INVALID_PLAYERS = "invalid_players";
    private static final String NOT_A_PLAYER = "not_a_player";
    private static final String NOT_A_PLAYER_MESSAGE = "Solo puedes puntuar las salas que has jugado";

    private final PlayDao playDao;
    private final UserDao userDao;
    private final RoomService roomService;

    public PlayService(PlayDao playDao, UserDao userDao, RoomService roomService) {
        this.playDao = playDao;
        this.userDao = userDao;
        this.roomService = roomService;
    }

    /** Replaces the list; removing someone keeps their grade. */
    @Transactional
    public RoomSummary replacePlayers(int roomId, PlayersRequest request, int userId) {
        if (request == null || request.userIds() == null || request.userIds().stream().anyMatch(Objects::isNull)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_PLAYERS, "Falta la lista de jugadores");
        }
        List<Integer> userIds = request.userIds().stream().distinct().sorted().toList();
        Set<Integer> known = userDao.findAll().stream().map(UserDTO::id).collect(Collectors.toSet());
        if (!known.containsAll(userIds)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_PLAYERS, "Algún jugador no existe");
        }
        roomService.requireExists(roomId);
        playDao.replacePlayers(roomId, userIds, request.playedOn());
        return roomService.summary(roomId, userId);
    }

    /** Only a player can grade the room. Every score empty removes my grade. */
    @Transactional
    public RoomSummary saveMyGrade(int roomId, Grade request, int userId) {
        Grade grade = GradeValidator.grade(request);
        roomService.requireExists(roomId);
        if (!playDao.isPlayer(roomId, userId)) {
            throw new ApiException(HttpStatus.CONFLICT, NOT_A_PLAYER, NOT_A_PLAYER_MESSAGE);
        }
        if (grade.hasAnyScore()) {
            playDao.saveGrade(roomId, userId, grade);
        } else {
            playDao.deleteGrade(roomId, userId);
        }
        return roomService.summary(roomId, userId);
    }
}
