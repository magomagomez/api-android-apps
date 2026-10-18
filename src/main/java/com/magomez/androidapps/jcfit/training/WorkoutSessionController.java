package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.JcfitApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = JcfitApi.BASE_URL + "/sessions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JCFit - sesiones", description = "Empezar un entreno, guardar series y terminarlo")
public class WorkoutSessionController {

    private final WorkoutSessionService sessionService;

    public WorkoutSessionController(WorkoutSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(summary = "Start a day (201), or get the session it already has (200); not a future day")
    @PostMapping
    public ResponseEntity<SessionReply> start(@RequestBody StartSessionRequest request) {
        StartedSession started = sessionService.start(request);
        return ResponseEntity.status(started.created() ? HttpStatus.CREATED : HttpStatus.OK).body(started.session());
    }

    @Operation(summary = "Insert or update a set by exercise and set number")
    @PutMapping("{sessionId}/sets")
    public LoggedSet saveSet(@PathVariable int sessionId, @RequestBody SaveSetRequest request) {
        return sessionService.saveSet(sessionId, request);
    }

    @Operation(summary = "Remove a set")
    @DeleteMapping("{sessionId}/sets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSet(@PathVariable int sessionId, @RequestParam int exerciseId, @RequestParam int setNumber) {
        sessionService.deleteSet(sessionId, exerciseId, setNumber);
    }

    @Operation(summary = "Finish the session; finishing again keeps the first time")
    @PostMapping("{sessionId}/finish")
    public SessionReply finish(@PathVariable int sessionId) {
        return sessionService.finish(sessionId);
    }
}
