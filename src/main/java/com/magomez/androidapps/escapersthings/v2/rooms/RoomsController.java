package com.magomez.androidapps.escapersthings.v2.rooms;

import com.magomez.androidapps.escapersthings.v2.auth.CurrentUser;
import com.magomez.androidapps.escapersthings.v2.config.ApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConfig.BASE_URL + "/escape-rooms")
@Tag(name = "Escapers v2 - salas", description = "Salas, jugadores y notas")
public class RoomsController {

    private final RoomService roomService;
    private final PlayService playService;

    public RoomsController(RoomService roomService, PlayService playService) {
        this.roomService = roomService;
        this.playService = playService;
    }

    @Operation(summary = "Every room by name, with group averages, players and my grade")
    @GetMapping
    public List<RoomSummary> list(@Parameter(hidden = true) @RequestAttribute(CurrentUser.ATTRIBUTE) CurrentUser user) {
        return roomService.list(user.id());
    }

    @Operation(summary = "A room with everyone's grades")
    @GetMapping("{roomId}")
    public RoomDetail get(@PathVariable int roomId,
                          @Parameter(hidden = true) @RequestAttribute(CurrentUser.ATTRIBUTE) CurrentUser user) {
        return roomService.get(roomId, user.id());
    }

    @Operation(summary = "Add a room")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomSummary create(@RequestBody SaveRoomRequest request,
                              @Parameter(hidden = true) @RequestAttribute(CurrentUser.ATTRIBUTE) CurrentUser user) {
        return roomService.create(request, user.id());
    }

    @Operation(summary = "Edit name, category and city")
    @PutMapping("{roomId}")
    public RoomSummary update(@PathVariable int roomId, @RequestBody SaveRoomRequest request,
                              @Parameter(hidden = true) @RequestAttribute(CurrentUser.ATTRIBUTE) CurrentUser user) {
        return roomService.update(roomId, request, user.id());
    }

    @Operation(summary = "Delete a room with its players, grades and photos")
    @DeleteMapping("{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int roomId) {
        roomService.delete(roomId);
    }

    @Operation(summary = "Who played the room and when (replaces the list; grades are kept)")
    @PutMapping("{roomId}/players")
    public RoomSummary players(@PathVariable int roomId, @RequestBody PlayersRequest request,
                               @Parameter(hidden = true) @RequestAttribute(CurrentUser.ATTRIBUTE) CurrentUser user) {
        return playService.replacePlayers(roomId, request, user.id());
    }

    @Operation(summary = "My five scores, each optional, 0-10 in quarter points (409 if I did not play it)")
    @PutMapping("{roomId}/grades/me")
    public RoomSummary myGrade(@PathVariable int roomId, @RequestBody Grade request,
                               @Parameter(hidden = true) @RequestAttribute(CurrentUser.ATTRIBUTE) CurrentUser user) {
        return playService.saveMyGrade(roomId, request, user.id());
    }
}
